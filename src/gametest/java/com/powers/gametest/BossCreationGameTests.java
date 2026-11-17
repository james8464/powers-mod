package com.powers.gametest;

import com.powers.PowersEntities;
import com.powers.PowersItems;
import com.powers.PowersWeapons;
import com.powers.item.ArtifactWeaponManager;
import com.powers.item.artifact.ArtifactAlignment;
import com.powers.player.PlayerPowers;
import com.powers.player.SkillSystem;
import com.powers.power.AbilityActivationService;
import com.powers.power.ActivationCooldowns;
import com.powers.power.crystals.CrystalPowerRegistry;
import com.powers.power.crystals.ModeCrystalAbility;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** Selected creation routes build real encounter terrain without damaging nearby bosses. */
public final class BossCreationGameTests {
	@GameTest
	@SuppressWarnings("removal")
	public void creationMissDoesNotPlaceOrCharge(GameTestHelper helper) {
		var caster = helper.makeMockServerPlayerInLevel();
		var origin = helper.absolutePos(new BlockPos(8, 3, 8));
		caster.snapTo(origin.getX() + 0.5, 200, origin.getZ() + 0.5, 0, -90);
		caster.setGameMode(GameType.SURVIVAL);
		caster.setNoGravity(true);
		caster.setItemInHand(InteractionHand.MAIN_HAND, PowersItems.ORANGE_CRYSTAL.getDefaultInstance());
		caster.setShiftKeyDown(true);
		caster.setPose(Pose.CROUCHING);
		helper.assertTrue(CrystalPowerRegistry.tryActivate(caster, PowersItems.ORANGE_CRYSTAL), "Creation mode selection failed");
		caster.setShiftKeyDown(false);
		caster.setPose(Pose.STANDING);
		var aim = caster.pick(16, 0, false);
		helper.assertTrue(aim.getType() == net.minecraft.world.phys.HitResult.Type.MISS, "Empty-sky fixture hit a block");
		var data = PlayerPowers.get(caster);
		data.forceRestoreEnergy();
		int energy = data.energy();
		helper.assertFalse(CrystalPowerRegistry.tryActivate(caster, PowersItems.ORANGE_CRYSTAL),
				"Creation accepted a miss as a placement anchor");
		helper.assertTrue(data.energy() == energy && ActivationCooldowns.remainingTicks(caster,
				CrystalPowerRegistry.get(PowersItems.ORANGE_CRYSTAL)) == 0, "Miss consumed energy or cooldown");
		var end = ((net.minecraft.world.phys.BlockHitResult) aim).getBlockPos();
		for (int x = -3; x <= 3; x++) for (int y = -3; y <= 3; y++) for (int z = -3; z <= 3; z++) {
			helper.assertTrue(helper.getLevel().getBlockState(end.offset(x, y, z)).isAir(), "Miss placed phantom terrain");
		}
		helper.succeed();
	}

	@GameTest public void orangeCreationNearLightHerald(GameTestHelper h) { create(h, PowersEntities.LIGHT_HERALD, null); }
	@GameTest public void orangeCreationNearDarkHerald(GameTestHelper h) { create(h, PowersEntities.DARK_HERALD, null); }
	@GameTest public void orangeCreationNearFirstVessel(GameTestHelper h) { create(h, PowersEntities.FIRST_VESSEL, null); }
	@GameTest public void lightCreationNearLightHerald(GameTestHelper h) { create(h, PowersEntities.LIGHT_HERALD, ArtifactAlignment.LIGHT); }
	@GameTest public void lightCreationNearDarkHerald(GameTestHelper h) { create(h, PowersEntities.DARK_HERALD, ArtifactAlignment.LIGHT); }
	@GameTest public void lightCreationNearFirstVessel(GameTestHelper h) { create(h, PowersEntities.FIRST_VESSEL, ArtifactAlignment.LIGHT); }
	@GameTest public void darkCreationNearLightHerald(GameTestHelper h) { create(h, PowersEntities.LIGHT_HERALD, ArtifactAlignment.DARKNESS); }
	@GameTest public void darkCreationNearDarkHerald(GameTestHelper h) { create(h, PowersEntities.DARK_HERALD, ArtifactAlignment.DARKNESS); }
	@GameTest public void darkCreationNearFirstVessel(GameTestHelper h) { create(h, PowersEntities.FIRST_VESSEL, ArtifactAlignment.DARKNESS); }

	@SuppressWarnings("removal")
	private static void create(GameTestHelper helper, EntityType<? extends Mob> type, ArtifactAlignment alignment) {
		var level = helper.getLevel();
		var relative = helper.absolutePos(new BlockPos(8, 3, 8));
		var center = new BlockPos(relative.getX(), 100, relative.getZ());
		for (int x = -3; x <= 3; x++) for (int y = 0; y <= 7; y++) for (int z = -3; z <= 3; z++) {
			level.setBlock(center.offset(x, y, z), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
		}
		level.setBlock(center.below(), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
		var caster = helper.makeMockServerPlayerInLevel();
		caster.setGameMode(GameType.SURVIVAL);
		caster.setNoGravity(true);
		caster.setPos(center.getX() + 0.5, 104, center.getZ() + 0.5);
		var data = PlayerPowers.get(caster);
		if (alignment != null) {
			if (alignment == ArtifactAlignment.DARKNESS) caster.addTag(SkillSystem.DARKNESS_TAG);
			data.setSkillLevel(caster, 1);
			data.setDarknessLevel(caster, 1);
			caster.setItemInHand(InteractionHand.MAIN_HAND, PowersWeapons.weapon(
					alignment == ArtifactAlignment.LIGHT ? "heavenly_partisan" : "lycanbane").getDefaultInstance());
			helper.assertTrue(ArtifactWeaponManager.select(caster, alignment, "crystal/creativity_manifestation", -1),
					"Artifact creation selection failed");
		} else {
			caster.setItemInHand(InteractionHand.MAIN_HAND, PowersItems.ORANGE_CRYSTAL.getDefaultInstance());
			caster.setShiftKeyDown(true);
			caster.setPose(Pose.CROUCHING);
			helper.assertTrue(CrystalPowerRegistry.tryActivate(caster, PowersItems.ORANGE_CRYSTAL), "Orange mode selection failed");
			caster.setShiftKeyDown(false);
			caster.setPose(Pose.STANDING);
			helper.assertTrue(((ModeCrystalAbility) CrystalPowerRegistry.get(PowersItems.ORANGE_CRYSTAL))
					.selectedActionId(caster).equals("creativity_manifestation"), "Wrong Orange mode selected");
		}
		caster.snapTo(caster.position(), 0, 90);
		var aim = caster.pick(16, 0, false);
		helper.assertTrue(aim.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK
				&& ((net.minecraft.world.phys.BlockHitResult) aim).getBlockPos().equals(center.below()),
				"Fixture ray did not hit its placement anchor: " + aim.getType() + " " + aim.getLocation());
		data.forceRestoreEnergy();
		int energy = data.energy();
		var boss = helper.spawn(type, new BlockPos(14, 3, 8));
		boss.setNoAi(true);
		boss.setNoGravity(true);
		boss.setPos(center.getX() + 6.5, 100, center.getZ() + 0.5);
		float health = boss.getHealth();
		if (alignment == null) helper.assertTrue(CrystalPowerRegistry.tryActivate(caster, PowersItems.ORANGE_CRYSTAL),
				"Orange creation cast refused clear placement");
		else helper.assertTrue(ArtifactWeaponManager.activateSelected(caster, alignment) == AbilityActivationService.Result.ACTIVATED,
				"Artifact creation cast refused clear placement");
		var ability = alignment == null ? CrystalPowerRegistry.get(PowersItems.ORANGE_CRYSTAL)
				: ArtifactWeaponManager.action(alignment, "crystal/creativity_manifestation").ability();
		helper.assertTrue(data.energy() < energy && ActivationCooldowns.remainingTicks(caster, ability) == 200,
				"Creation bypassed payment or cooldown");
		helper.assertTrue(level.getBlockState(center.offset(-2, 0, -2)).is(Blocks.CONCRETE.pick(DyeColor.ORANGE))
				&& level.getBlockState(center.above(2)).is(Blocks.GLOWSTONE), "Selected route did not place the authored chamber");
		helper.runAfterDelay(5, () -> {
			helper.assertTrue(boss.isAlive() && boss.getHealth() == health, "Creation damaged an adjacent encounter boss");
			helper.succeed();
		});
	}
}
