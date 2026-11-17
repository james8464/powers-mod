package com.powers.gametest;

import com.powers.PowersEntities;
import com.powers.testing.BossActionResponseMatrix;
import com.powers.power.PowerRegistry;
import com.powers.power.crystals.CrystalAbilityCatalog;
import com.powers.spell.SpellRegistry;
import com.powers.item.artifact.ArtifactActionCatalogue;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;

/** Inventory/persistence smoke gate; individual live cast tests must prove the declared responses. */
public final class BossActionResponseMatrixGameTests {
	@GameTest(maxTicks = 20)
	public void everyPlayerActionHasALiveBossResponseContract(GameTestHelper helper) {
		PowerRegistry.initialize();
		var vessel = helper.spawn(PowersEntities.FIRST_VESSEL, new BlockPos(2, 1, 2));
		var light = helper.spawn(PowersEntities.LIGHT_HERALD, new BlockPos(5, 1, 2));
		var dark = helper.spawn(PowersEntities.DARK_HERALD, new BlockPos(8, 1, 2));
		var matrix = BossActionResponseMatrix.generated();
		helper.assertTrue(vessel.isPersistenceRequired() && light.isPersistenceRequired()
				&& dark.isPersistenceRequired(), "Boss response matrix lacks a live persistent host");
		long actionCount = PowerRegistry.getAll().size()
				+ SpellRegistry.defaults().definitions().stream().mapToLong(book -> book.spells().size()).sum()
				+ CrystalAbilityCatalog.defaults().values().stream().flatMap(java.util.List::stream).distinct().count()
				+ ArtifactActionCatalogue.all().size();
		helper.assertTrue(matrix.size() == actionCount
				* BossActionResponseMatrix.Boss.values().length,
				"Generated boss response matrix omitted a player action");
		helper.assertTrue(matrix.stream().noneMatch(entry -> entry.response()
				== BossActionResponseMatrix.Response.UNCLASSIFIED),
				"Generated boss response matrix contains an unclassified action");
		helper.succeed();
	}
}
