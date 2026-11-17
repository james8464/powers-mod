package com.powers.gametest;

import com.powers.PowersEntities;
import com.powers.PowersItems;
import com.powers.PowersWeapons;
import com.powers.entity.FirstVessel;
import com.powers.item.ArtifactWeaponManager;
import com.powers.item.artifact.ArtifactAlignment;
import com.powers.player.PlayerPowers;
import com.powers.player.SkillSystem;
import com.powers.power.AbilityActivationService;
import com.powers.power.ActivationCooldowns;
import com.powers.power.crystals.CrystalPowerRegistry;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

/** Registered crystal/relic routing must deliver damage and retire its finite firestorm. */
public final class BossInfernoGameTests {
	@GameTest(maxTicks = 300)
	public void redCrystalBurnsLightHerald(GameTestHelper helper) { inferno(helper, PowersEntities.LIGHT_HERALD, false); }
	@GameTest(maxTicks = 300)
	public void redCrystalBurnsDarkHerald(GameTestHelper helper) { inferno(helper, PowersEntities.DARK_HERALD, false); }
	@GameTest(maxTicks = 300)
	public void redCrystalBurnsFirstVessel(GameTestHelper helper) { inferno(helper, PowersEntities.FIRST_VESSEL, false); }
	@GameTest(maxTicks = 300)
	public void darkArtifactBurnsLightHerald(GameTestHelper helper) { inferno(helper, PowersEntities.LIGHT_HERALD, true); }
	@GameTest(maxTicks = 300)
	public void darkArtifactBurnsDarkHerald(GameTestHelper helper) { inferno(helper, PowersEntities.DARK_HERALD, true); }
	@GameTest(maxTicks = 300)
	public void darkArtifactBurnsFirstVessel(GameTestHelper helper) { inferno(helper, PowersEntities.FIRST_VESSEL, true); }

	@SuppressWarnings("removal")
	private static void inferno(GameTestHelper helper, EntityType<? extends Mob> type, boolean artifact) {
		for (int x = 1; x <= 9; x++) for (int y = 6; y <= 12; y++) {
			for (int z = 1; z <= 9; z++) helper.setBlock(new BlockPos(x, y, z), Blocks.AIR);
		}
		var caster = helper.makeMockServerPlayerInLevel();
		caster.setGameMode(GameType.SURVIVAL);
		var origin = helper.absolutePos(new BlockPos(4, 8, 4));
		caster.setPos(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
		caster.setNoGravity(true);
		var data = PlayerPowers.get(caster);
		if (artifact) {
			caster.addTag(SkillSystem.DARKNESS_TAG);
			data.setDarknessLevel(caster, 1);
			caster.setItemInHand(InteractionHand.MAIN_HAND, PowersWeapons.weapon("lycanbane").getDefaultInstance());
		} else caster.setItemInHand(InteractionHand.MAIN_HAND, PowersItems.RED_CRYSTAL.getDefaultInstance());
		data.forceRestoreEnergy();
		var boss = helper.spawn(type, new BlockPos(4, 8, 7));
		boss.setNoAi(true);
		boss.setNoGravity(true);
		float before = vitality(boss);
		int energy = data.energy();
		var ability = artifact ? ArtifactWeaponManager.action(ArtifactAlignment.DARKNESS, "crystal/inferno").ability()
				: CrystalPowerRegistry.get(PowersItems.RED_CRYSTAL);
		if (artifact) {
			helper.assertTrue(ArtifactWeaponManager.select(caster, ArtifactAlignment.DARKNESS, "crystal/inferno", -1),
					"Held Darkness artifact could not select Inferno");
			helper.assertTrue(ArtifactWeaponManager.activateSelected(caster, ArtifactAlignment.DARKNESS)
					== AbilityActivationService.Result.ACTIVATED, "Artifact Inferno failed");
		} else helper.assertTrue(CrystalPowerRegistry.tryActivate(caster, PowersItems.RED_CRYSTAL), "Red Crystal failed");
		helper.assertTrue(data.energy() < energy && ActivationCooldowns.remainingTicks(caster, ability) == 1800,
				"Inferno bypassed its payment or 90-second cooldown");
		helper.runAfterDelay(20, () -> helper.assertTrue(vitality(boss) < before && boss.isOnFire(),
				"Inferno did not damage and ignite the boss: " + vitality(boss) + "/" + before));
		// Darkness baseline lasts 160 * 1.4 = 224 ticks; the Red Crystal lasts 160.
		if (artifact) helper.runAfterDelay(205, () -> {
			boss.clearFire();
			float nearEnd = vitality(boss);
			helper.runAfterDelay(12, () -> helper.assertTrue(boss.isOnFire() && vitality(boss) < nearEnd,
					"Artifact Inferno stopped before its extended 224-tick lease"));
		});
		helper.runAfterDelay(artifact ? 230 : 166, () -> {
			boss.clearFire();
			float after = vitality(boss);
			helper.runAfterDelay(25, () -> {
				helper.assertTrue(boss.isAlive() && !boss.isOnFire() && vitality(boss) == after,
						"Expired Inferno continued to ignite or damage its boss");
				helper.succeed();
			});
		});
	}

	private static float vitality(Mob boss) {
		return boss instanceof FirstVessel vessel ? vessel.effectiveHealth() : boss.getHealth();
	}
}
