package com.powers.gametest;

import com.powers.ImportedPackItems;
import com.powers.PowersEntities;
import com.powers.player.PlayerPowers;
import com.powers.spell.CelestialRuinManager;
import com.powers.spell.SpellCastingManager;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Exercises the real spell channel, warning window and catastrophic boss damage. */
public final class BossHeavenfallGameTests {
	@GameTest(maxTicks = 2400)
	@SuppressWarnings("removal")
	public void heavenfallCompletesItsWarningBeforeStrikingAllThreeBosses(GameTestHelper helper) {
		var caster = helper.makeMockServerPlayerInLevel();
		caster.setGameMode(GameType.SURVIVAL);
		caster.snapTo(Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(2, 1, 2))), 0F, 0F);
		caster.setNoGravity(true);
		helper.setBlock(new BlockPos(2, 2, 5), Blocks.STONE);
		caster.setItemInHand(InteractionHand.MAIN_HAND,
				ImportedPackItems.item("imported_book_grimoire_celestial").getDefaultInstance());
		var data = PlayerPowers.get(caster);
		data.setSelectedSpell("book_grimoire_celestial", 3);
		data.forceRestoreEnergy();
		for (int x = 3; x <= 10; x++) for (int y = 1; y <= 8; y++) {
			for (int z = 6; z <= 10; z++) helper.setBlock(new BlockPos(x, y, z), Blocks.AIR);
		}
		var bosses = java.util.List.of(
				helper.spawn(PowersEntities.LIGHT_HERALD, new BlockPos(4, 2, 7)),
				helper.spawn(PowersEntities.DARK_HERALD, new BlockPos(6, 2, 7)),
				helper.spawn(PowersEntities.FIRST_VESSEL, new BlockPos(8, 2, 7)));
		bosses.forEach(boss -> { boss.setNoAi(true); boss.setNoGravity(true); boss.setHealth(37F); });
		int energy = data.energy();
		long readyAt = helper.getLevel().getGameTime() + 72_000;
		SpellCastingManager.use(caster, "book_grimoire_celestial");
		helper.assertTrue(SpellCastingManager.isChanneling(caster.getUUID())
				&& data.energy() == energy - 100 && data.cooldownReadyAt("spell:celestial_ruin") == readyAt,
				"Heavenfall did not reserve its authored payment and channel");
		helper.runAfterDelay(190, () -> helper.assertTrue(
				SpellCastingManager.isChanneling(caster.getUUID())
				&& CelestialRuinManager.activeRitualCount(helper.getLevel().getServer()) == 0,
				"Heavenfall skipped its 200-tick spell channel"));
		helper.runAfterDelay(215, () -> {
			helper.assertTrue(!SpellCastingManager.isChanneling(caster.getUUID())
					&& CelestialRuinManager.activeRitualCount(helper.getLevel().getServer()) == 1,
					"Completed channel did not start its persistent warning");
			// Keep the test observer alive; bosses remain ordinary damageable entities.
			caster.setGameMode(GameType.CREATIVE);
		});
		helper.runAfterDelay(1350, () -> helper.assertTrue(
				bosses.stream().allMatch(boss -> boss.isAlive() && boss.getHealth() == 37F),
				"Boss state changed during warning: " + bosses.stream().map(boss ->
						boss.getName().getString() + " alive=" + boss.isAlive() + " hp=" + boss.getHealth()
						+ " pos=" + boss.position() + " hurt=" + boss.getLastDamageSource()).toList()));
		helper.runAfterDelay(1420, () -> helper.succeedWhen(() -> {
			helper.assertTrue(bosses.stream().noneMatch(boss -> boss.isAlive()),
					"Completed Heavenfall did not kill all three unshielded bosses");
			helper.assertTrue(data.cooldownReadyAt("spell:celestial_ruin") == readyAt,
					"Catastrophic ritual lost its committed cooldown");
		}));
	}
}
