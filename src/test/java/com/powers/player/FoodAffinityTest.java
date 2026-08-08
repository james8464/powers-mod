package com.powers.player;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import net.minecraft.resources.Identifier;

class FoodAffinityTest {
	@Test
	void classifiesVanillaRottenFleshAsAbnormal() {
		assertEquals(FoodAffinity.ABNORMAL,
				FoodAffinity.of(Identifier.fromNamespaceAndPath("minecraft", "rotten_flesh")));
	}

	@Test
	void doesNotMisclassifyAnotherModsMatchingPath() {
		assertEquals(FoodAffinity.NORMAL,
				FoodAffinity.of(Identifier.fromNamespaceAndPath("example", "rotten_flesh")));
	}
}
