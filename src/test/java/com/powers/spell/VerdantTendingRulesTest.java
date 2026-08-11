package com.powers.spell;

import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VerdantTendingRulesTest {
	@BeforeAll
	static void bootstrapMinecraftRegistries() {
		net.minecraft.SharedConstants.tryDetectVersion();
		net.minecraft.server.Bootstrap.bootStrap();
	}

	@Test
	void classifiesOnlyAuthoredRestorationTargets() {
		assertEquals(VerdantTendingRules.Action.GROW,
				VerdantTendingRules.action(Blocks.WHEAT.defaultBlockState()));
		assertEquals(VerdantTendingRules.Action.HYDRATE,
				VerdantTendingRules.action(Blocks.FARMLAND.defaultBlockState()));
		assertEquals(VerdantTendingRules.Action.EXTINGUISH,
				VerdantTendingRules.action(Blocks.FIRE.defaultBlockState()));
		assertEquals(VerdantTendingRules.Action.NONE,
				VerdantTendingRules.action(Blocks.STONE.defaultBlockState()));
	}

	@Test
	void workCapsAreFiniteAndIndependent() {
		assertEquals(192, VerdantTendingRules.MAX_INSPECTED_BLOCKS);
		assertEquals(64, VerdantTendingRules.MAX_CHANGED_BLOCKS);
	}
}
