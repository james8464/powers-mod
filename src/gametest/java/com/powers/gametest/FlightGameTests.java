package com.powers.gametest;

import com.powers.PowersEntities;
import com.powers.PowersWeapons;
import com.powers.item.ArtifactWeaponManager;
import com.powers.item.artifact.ArtifactAlignment;
import com.powers.player.PlayerPowers;
import com.powers.player.SkillSystem;
import com.powers.power.AbilityActivationService;
import com.powers.power.abilities.FlightAbility;
import com.powers.power.abilities.FlightRules;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

/** Live server proof for PWR-006 survival propulsion and bounded stale-input braking. */
public final class FlightGameTests {
	@GameTest
	public void darknessArtifactFlightSupportsCasterNearBosses(GameTestHelper helper) {
		verifyArtifactFlight(helper, ArtifactAlignment.DARKNESS, "lycanbane");
	}

	@GameTest
	public void lightArtifactFlightSupportsCasterNearBosses(GameTestHelper helper) {
		verifyArtifactFlight(helper, ArtifactAlignment.LIGHT, "heavenly_partisan");
	}

	@SuppressWarnings("removal")
	private static void verifyArtifactFlight(GameTestHelper helper, ArtifactAlignment alignment, String weapon) {
		var player = helper.makeMockServerPlayerInLevel();
		var origin = helper.absolutePos(new net.minecraft.core.BlockPos(4, 3, 4));
		player.setPos(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
		var data = PlayerPowers.get(player);
		data.setSkillLevel(player, 1);
		data.setDarknessLevel(player, 1);
		if (alignment == ArtifactAlignment.DARKNESS) player.addTag(SkillSystem.DARKNESS_TAG);
		player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, PowersWeapons.weapon(weapon).getDefaultInstance());
		data.forceRestoreEnergy();
		var bosses = java.util.List.of(
				helper.spawn(PowersEntities.DARK_HERALD, new net.minecraft.core.BlockPos(3, 3, 4)),
				helper.spawn(PowersEntities.LIGHT_HERALD, new net.minecraft.core.BlockPos(4, 3, 3)),
				helper.spawn(PowersEntities.FIRST_VESSEL, new net.minecraft.core.BlockPos(4, 3, 6)));
		var health = bosses.stream().map(boss -> boss.getHealth()).toList();
		boolean mayFly = player.getAbilities().mayfly;
		boolean flying = player.getAbilities().flying;
		String action = "innate/flight";
		String toggle = "artifact/" + alignment.serializedName() + "/" + action;
		helper.assertTrue(ArtifactWeaponManager.select(player, alignment, action, -1), "Artifact refused Flight selection");
		helper.assertTrue(ArtifactWeaponManager.activateSelected(player, alignment) == AbilityActivationService.Result.ACTIVATED
				&& data.isToggleActive(toggle) && data.flightSnapshot() == 0, "Artifact Flight did not start");
		helper.assertTrue(ArtifactWeaponManager.activateSelected(player, alignment) == AbilityActivationService.Result.ACTIVATED
				&& !data.isToggleActive(toggle) && data.flightSnapshot() == -1
				&& player.hasEffect(net.minecraft.world.effect.MobEffects.SLOW_FALLING),
				"Artifact Flight did not clear its state and grant a safe descent");
		helper.assertTrue(player.getAbilities().mayfly == mayFly && player.getAbilities().flying == flying,
				"Artifact Flight changed vanilla flight flags");
		helper.assertTrue(bosses.stream().map(boss -> boss.getHealth()).toList().equals(health),
				"Artifact Flight changed boss health");
		helper.succeed();
	}

	@GameTest(maxTicks = 20)
	@SuppressWarnings("removal")
	public void pwr006MomentumFlightRemainsServerOwnedAndBoundsClientIntent(
			GameTestHelper helper) {
		var player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		GameType beforeMode = player.gameMode();
		var data = PlayerPowers.get(player);
		var flight = new FlightAbility();
		helper.assertTrue(flight.activateToggleOn(player, data), "Flight did not activate");
		Vec3 before = new Vec3(3.5, 0.6, 0.0);
		player.setDeltaMovement(before);

		flight.tickActive(player, data);
		Vec3 after = player.getDeltaMovement();
		helper.assertTrue(after.distanceTo(before) <= Math.sqrt(2.0) *
				FlightRules.MAX_AIR_ACCELERATION + 0.0001,
				"One retained client-input tick exceeded the server acceleration bound");
		helper.assertTrue(Double.isFinite(after.x) && Double.isFinite(after.y)
				&& Double.isFinite(after.z), "Flight produced invalid server velocity");
		helper.assertTrue(player.gameMode() == beforeMode,
				"Flight propulsion changed the player's game mode");
		flight.activateToggleOff(player, data);
		helper.assertTrue(data.flightSnapshot() == -1, "Flight cleanup left session state behind");
		helper.succeed();
	}
}
