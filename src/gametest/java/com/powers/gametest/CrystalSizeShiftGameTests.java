package com.powers.gametest;

import com.powers.player.PlayerPowers;
import com.powers.power.crystals.CrystalSizeShiftRules;
import com.powers.power.crystals.SizeShiftAbility;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** Live Yellow Crystal collision, hitbox, shockwave, and mount rejection proof. */
public final class CrystalSizeShiftGameTests {
	@GameTest(maxTicks = 40, padding = 48)
	@SuppressWarnings("removal")
	public void yellowSizeShiftPreviewsCollisionAndSynchronizesItsBody(GameTestHelper helper) {
		var player = helper.makeMockServerPlayerInLevel();
		BlockPos fixture = helper.absolutePos(new BlockPos(8, 3, 8));
		BlockPos origin = new BlockPos(fixture.getX(), 100, fixture.getZ());
		player.setPos(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
		player.setYRot(0.0F);
		player.setXRot(0.0F);
		SizeShiftAbility ability = new SizeShiftAbility();

		helper.assertTrue(ability.activate(player, PlayerPowers.get(player)),
				"Clear first transition to the small Yellow form failed");
		helper.assertTrue(Math.abs(player.getScale() - CrystalSizeShiftRules.SMALL_SCALE) < 0.0001,
				"Small Yellow scale did not reach the authoritative hitbox");
		BlockPos ceiling = origin.above(3);
		helper.getLevel().setBlock(ceiling, Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
		helper.assertFalse(ability.activate(player, PlayerPowers.get(player)),
				"Tenfold transition ignored the collision preview");
		helper.assertTrue(Math.abs(player.getScale() - CrystalSizeShiftRules.SMALL_SCALE) < 0.0001,
				"Rejected transition mutated the existing safe body");

		helper.getLevel().setBlock(ceiling, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
		var target = helper.spawn(EntityTypes.ZOMBIE, new BlockPos(11, 3, 8));
		target.setPos(origin.getX() + 3.0, origin.getY(), origin.getZ() + 0.5);
		helper.assertTrue(ability.activate(player, PlayerPowers.get(player)),
				"Clear tenfold transition failed after obstruction removal");
		helper.assertTrue(Math.abs(player.getScale() - CrystalSizeShiftRules.GIANT_SCALE) < 0.0001,
				"Giant Yellow scale did not reach the authoritative hitbox");
		double width = player.getBoundingBox().getXsize();
		helper.assertTrue(width > 5.9 && width < 6.1 && helper.getLevel().noCollision(player),
				"Giant scale and live collision box desynchronized");
		helper.assertTrue(target.getDeltaMovement().horizontalDistanceSqr() > 0.0,
				"Visible giant transition shockwave did not move a nearby target");
		helper.succeed();
	}

	@GameTest(maxTicks = 20)
	@SuppressWarnings("removal")
	public void yellowSizeShiftRejectsMountedActivationWithoutMutation(GameTestHelper helper) {
		var player = helper.makeMockServerPlayerInLevel();
		var horse = helper.spawn(EntityTypes.HORSE, new BlockPos(4, 3, 4));
		helper.assertTrue(player.startRiding(horse, true, true), "Mount fixture failed");
		helper.assertFalse(new SizeShiftAbility().activate(player, PlayerPowers.get(player)),
				"Mounted Yellow activation was not rejected");
		helper.assertTrue(player.isPassenger() && Math.abs(player.getScale() - 1.0) < 0.0001,
				"Mounted rejection changed the mount graph or player scale");
		helper.succeed();
	}
}
