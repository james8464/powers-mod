package com.powers.gametest;

import com.powers.AmethystWardBlock;
import com.powers.ImportedPackItems;
import com.powers.PowersBlocks;
import com.powers.PowersEntities;
import com.powers.player.PlayerPowers;
import com.powers.power.AmethystDampening;
import com.powers.spell.SpellCastingManager;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Blocks;

/** Full Ward Breaking casts preserve the ward block and restore its field after expiry. */
public final class BossWardRitualGameTests {
	@GameTest(maxTicks = 1050, padding = 64)
	public void wardBreakingNearLightHeraldExpires(GameTestHelper helper) {
		ward(helper, PowersEntities.LIGHT_HERALD);
	}
	@GameTest(maxTicks = 1050, padding = 64)
	public void wardBreakingNearDarkHeraldExpires(GameTestHelper helper) {
		ward(helper, PowersEntities.DARK_HERALD);
	}
	@GameTest(maxTicks = 1050, padding = 64)
	public void wardBreakingNearFirstVesselExpires(GameTestHelper helper) {
		ward(helper, PowersEntities.FIRST_VESSEL);
	}

	@SuppressWarnings("removal")
	private static void ward(GameTestHelper helper, EntityType<? extends Mob> type) {
		var caster = helper.makeMockServerPlayerInLevel();
		var origin = helper.absolutePos(new BlockPos(4, 2, 4));
		caster.setPos(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
		caster.setNoGravity(true);
		caster.setYRot(0);
		caster.setXRot(0);
		caster.setItemInHand(InteractionHand.MAIN_HAND,
				ImportedPackItems.item("imported_book_grimoire_abyssal").getDefaultInstance());
		var data = PlayerPowers.get(caster);
		data.setSelectedSpell("book_grimoire_abyssal", 0);
		data.forceRestoreEnergy();
		BlockPos ward = new BlockPos(4, 3, 28);
		// The distant ward is beyond the template wall; provide a real unobstructed sightline.
		for (int z = 5; z < 28; z++) {
			helper.setBlock(new BlockPos(4, 3, z), Blocks.AIR);
			helper.setBlock(new BlockPos(4, 4, z), Blocks.AIR);
		}
		for (BlockPos pos : BlockPos.betweenClosed(5, 2, 26, 7, 5, 28)) helper.setBlock(pos, Blocks.AIR);
		helper.setBlock(ward.below(), Blocks.REDSTONE_BLOCK);
		helper.setBlock(ward, PowersBlocks.AMETHYST_WARD);
		var boss = helper.spawn(type, new BlockPos(6, 2, 27));
		boss.setNoAi(true);
		boss.setNoGravity(true);
		boss.setHealth(37F);
		helper.runAfterDelay(5, () -> {
			helper.assertTrue(AmethystWardBlock.isPowered(helper.getBlockState(ward))
					&& AmethystDampening.findPoweredWard(helper.getLevel(), boss.blockPosition()).isPresent()
					&& AmethystDampening.findPoweredWard(helper.getLevel(), caster.blockPosition()).isEmpty(),
					"Ward fixture must suppress the boss but leave its distant caster outside the field");
			int energy = data.energy();
			long cooldown = caster.level().getGameTime() + 1200;
			var messages = BossControlResponseGameTests.capture(caster);
			var hit = caster.pick(32, 0, false);
			SpellCastingManager.use(caster, "book_grimoire_abyssal");
			helper.assertTrue(SpellCastingManager.isChanneling(caster.getUUID())
					&& data.energy() == energy - 26
					&& data.cooldownReadyAt("spell:ward_breaking_ritual") == cooldown,
					"Ward Breaking did not acquire the ward and reserve channel/payment/cooldown: hit="
							+ hit.getType() + "@" + hit.getLocation() + ", eye=" + caster.getEyePosition()
							+ ", energy=" + data.energy() + "/" + energy + ", feedback="
							+ messages.stream().map(packet -> packet.content().getString()).toList());
			helper.runAfterDelay(85, () -> {
				helper.assertTrue(!SpellCastingManager.isChanneling(caster.getUUID())
						&& AmethystDampening.findPoweredWard(helper.getLevel(), boss.blockPosition()).isEmpty(),
						"Completed Ward Breaking did not suppress the field around the boss");
				helper.assertTrue(AmethystWardBlock.isPowered(helper.getBlockState(ward))
						&& boss.getHealth() == 37F
						&& data.cooldownReadyAt("spell:ward_breaking_ritual") == cooldown,
						"Ward Breaking destroyed the ward, damaged the boss or rolled back its cooldown");
			});
			helper.runAfterDelay(970, () -> helper.assertTrue(
					AmethystDampening.findPoweredWard(helper.getLevel(), boss.blockPosition()).isEmpty(),
					"Ward suppression expired before its 900-tick duration"));
			helper.runAfterDelay(990, () -> {
				helper.assertTrue(AmethystDampening.findPoweredWard(helper.getLevel(), boss.blockPosition()).isPresent()
						&& AmethystWardBlock.isPowered(helper.getBlockState(ward)),
						"Ward suppression became permanent instead of restoring its field");
			helper.succeed();
			});
		});
	}
}
