package com.powers.player;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.powers.allegiance.AllegianceMatrix;

import org.junit.jupiter.api.Test;

import net.minecraft.resources.Identifier;

class FoodAffinityTest {
	@Test
	void classifiesVanillaRottenFleshAsAbnormal() {
		assertEquals(FoodAffinity.ABNORMAL,
				FoodAffinity.of(Identifier.fromNamespaceAndPath("minecraft", "rotten_flesh")));
	}

	@Test
	void unknownThirdPartyFoodDefaultsNeutralEvenWhenItsPathMatchesVanilla() {
		assertEquals(FoodAffinity.NEUTRAL,
				FoodAffinity.of(Identifier.fromNamespaceAndPath("example", "rotten_flesh")));
	}

	@Test
	void authoredFoodClassesMapIntoTheSharedAllegianceMatrix() {
		assertEquals(AllegianceMatrix.Faction.RADIANT, FoodAffinity.faction(FoodAffinity.NORMAL));
		assertEquals(AllegianceMatrix.Faction.DARKNESS, FoodAffinity.faction(FoodAffinity.ABNORMAL));
		assertEquals(AllegianceMatrix.Faction.NEUTRAL, FoodAffinity.faction(FoodAffinity.NEUTRAL));
	}

	@Test
	void unknownThirdPartyFoodDefaultsNeutral() {
		assertEquals(FoodAffinity.NEUTRAL,
				FoodAffinity.of(Identifier.fromNamespaceAndPath("example", "moon_pie")));
	}

	@Test
	void unlistedVanillaFoodRetainsNormalFallback() {
		assertEquals(FoodAffinity.NORMAL,
				FoodAffinity.of(Identifier.fromNamespaceAndPath("minecraft", "cake")));
	}
}
