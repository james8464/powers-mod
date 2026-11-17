package com.powers.gametest;

import com.powers.PowersEntities;
import com.powers.fx.VisualScarService;
import com.powers.player.PlayerPowers;
import com.powers.power.AbilityActivationService;
import com.powers.power.abilities.EnergyBeamAbility;
import com.powers.testing.TestingOverrides;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

/** Live proof that Energy Beam visuals outpace discrete damage and leave only transient scorch. */
public final class EnergyBeamGameTests {
	@GameTest(maxTicks = 40)
	public void darknessArtifactBeamHitsLightHerald(GameTestHelper helper) {
		artifactBeam(helper, com.powers.item.artifact.ArtifactAlignment.DARKNESS, PowersEntities.LIGHT_HERALD);
	}

	@GameTest(maxTicks = 40)
	public void darknessArtifactBeamHitsDarkHerald(GameTestHelper helper) {
		artifactBeam(helper, com.powers.item.artifact.ArtifactAlignment.DARKNESS, PowersEntities.DARK_HERALD);
	}

	@GameTest(maxTicks = 40)
	public void darknessArtifactBeamHitsFirstVessel(GameTestHelper helper) {
		artifactBeam(helper, com.powers.item.artifact.ArtifactAlignment.DARKNESS, PowersEntities.FIRST_VESSEL);
	}

	@GameTest(maxTicks = 40)
	public void lightArtifactBeamHitsLightHerald(GameTestHelper helper) {
		artifactBeam(helper, com.powers.item.artifact.ArtifactAlignment.LIGHT, PowersEntities.LIGHT_HERALD);
	}

	@GameTest(maxTicks = 40)
	public void lightArtifactBeamHitsDarkHerald(GameTestHelper helper) {
		artifactBeam(helper, com.powers.item.artifact.ArtifactAlignment.LIGHT, PowersEntities.DARK_HERALD);
	}

	@GameTest(maxTicks = 40)
	public void lightArtifactBeamHitsFirstVessel(GameTestHelper helper) {
		artifactBeam(helper, com.powers.item.artifact.ArtifactAlignment.LIGHT, PowersEntities.FIRST_VESSEL);
	}

	@SuppressWarnings("removal")
	private static void artifactBeam(GameTestHelper helper, com.powers.item.artifact.ArtifactAlignment alignment,
			net.minecraft.world.entity.EntityType<? extends net.minecraft.world.entity.Mob> type) {
		var caster = helper.makeMockServerPlayerInLevel();
		caster.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
		caster.setNoGravity(true);
		var origin = helper.absolutePos(new BlockPos(3, 4, 3));
		caster.snapTo(origin.getX() + 0.5, 100, origin.getZ() + 0.5, 0, 0);
		var data = PlayerPowers.get(caster);
		data.setSkillLevel(caster, 1);
		data.setDarknessLevel(caster, 1);
		boolean darkness = alignment == com.powers.item.artifact.ArtifactAlignment.DARKNESS;
		if (darkness) caster.addTag(com.powers.player.SkillSystem.DARKNESS_TAG);
		caster.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
				com.powers.PowersWeapons.weapon(darkness ? "lycanbane" : "heavenly_partisan").getDefaultInstance());
		data.forceRestoreEnergy();
		int energy = data.energy();
		var boss = helper.spawn(type, new BlockPos(3, 4, 7));
		boss.setNoAi(true);
		boss.setNoGravity(true);
		boss.setPos(caster.getX(), 100, caster.getZ() + 4);
		double initial = vitality(boss);
		helper.assertTrue(com.powers.item.ArtifactWeaponManager.select(caster, alignment, "innate/energy_beam", -1),
				"Artifact refused Energy Beam selection");
		helper.assertTrue(com.powers.item.ArtifactWeaponManager.activateSelected(caster, alignment)
				== AbilityActivationService.Result.ACTIVATED && data.energy() < energy,
				"Artifact Energy Beam activation did not succeed and spend energy");
		helper.runAfterDelay(24, () -> {
			try {
				var diagnostics = EnergyBeamAbility.diagnosticsForTest(caster.getUUID());
				helper.assertTrue(diagnostics.damageBeats() == 2 && vitality(boss) < initial,
						"Energy Beam did not damage the boss on its two authoritative beats: " + diagnostics);
				helper.succeed();
			} finally {
				EnergyBeamAbility.clear(caster.getUUID());
			}
		});
	}

	private static double vitality(net.minecraft.world.entity.Mob boss) {
		return boss instanceof com.powers.entity.FirstVessel vessel ? vessel.effectiveHealth() : boss.getHealth();
	}

	@GameTest(maxTicks = 40)
	@SuppressWarnings("removal")
	public void liveAimRefreshesEveryTickWhileDamageAndScorchStayDiscrete(GameTestHelper helper) {
		var caster = helper.makeMockServerPlayerInLevel();
		BlockPos origin = helper.absolutePos(new BlockPos(2, 4, 2));
		caster.setPos(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
		caster.setNoGravity(true);
		caster.setYRot(0.0F);
		caster.setXRot(0.0F);
		PlayerPowers.get(caster).setSlots(caster, List.of(
				"powers:energy_beam", "powers:size_shift", "powers:double_health"));
		TestingOverrides.setEnergyDisabled(caster.getUUID(), true);
		EnergyBeamAbility.clear(caster.getUUID());
		var target = helper.spawn(PowersEntities.POWER_TEST_ACTOR, new BlockPos(2, 4, 8));
		target.setNoAi(true);
		target.setNoGravity(true);
		helper.setBlock(new BlockPos(2, 3, 8), Blocks.OBSIDIAN);
		float initialHealth = target.getHealth();
		EnergyBeamAbility ability = new EnergyBeamAbility();
		helper.assertTrue(AbilityActivationService.activate(caster, ability,
				ability.id().toString()) == AbilityActivationService.Result.ACTIVATED,
				"Energy Beam did not enter its production activation pipeline");

		helper.runAfterDelay(24, () -> {
			try {
				EnergyBeamAbility.ChannelDiagnostics diagnostics =
						EnergyBeamAbility.diagnosticsForTest(caster.getUUID());
				helper.assertTrue(diagnostics.damageBeats() == 2,
						"Energy Beam did not retain its exact ten-tick damage cadence: " + diagnostics);
				helper.assertTrue(diagnostics.visualFrames() >= diagnostics.damageBeats() * 8,
						"Live-aim presentation did not refresh between damage beats: " + diagnostics);
				helper.assertTrue(diagnostics.scorchRequests() == diagnostics.damageBeats(),
						"Accepted body hits did not request one temporary scorch each: " + diagnostics);
				helper.assertTrue(target.getHealth() < initialHealth,
						"The discrete authoritative beats dealt no damage");
				helper.assertTrue(helper.getBlockState(new BlockPos(2, 3, 8)).is(Blocks.OBSIDIAN),
						"The temporary body-hit scorch mutated its supporting block");
				helper.assertTrue(VisualScarService.diagnosticsForTest(
						helper.getLevel().getServer(), caster.getUUID()).activeCount() > 0,
						"The accepted body hit produced no transient scorch decal");
				helper.succeed();
			} finally {
				EnergyBeamAbility.clear(caster.getUUID());
				TestingOverrides.clear(caster.getUUID());
			}
		});
	}
}
