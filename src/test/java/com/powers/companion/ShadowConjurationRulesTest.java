package com.powers.companion;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShadowConjurationRulesTest {
	@Test
	void ordinaryTiersAreBoundedToOnePlainStack() {
		assertDecision(ShadowConjurationTier.COMMON, 4, facts(80, true, false));
		assertDecision(ShadowConjurationTier.UNCOMMON, 12,
				facts(16, true, false).withTier(ShadowConjurationTier.UNCOMMON));
		assertDecision(ShadowConjurationTier.RARE, 40,
				facts(1, true, false).withTier(ShadowConjurationTier.RARE));
		assertDecision(ShadowConjurationTier.MYTHIC, 250,
				facts(1, true, false).withTier(ShadowConjurationTier.MYTHIC));
		assertEquals(64, ShadowConjurationRules.evaluate(facts(80, true, false)).boundedCount());
	}

	@Test
	void forbiddenAndThirdPartyItemsNeverSlipThroughTestingBypass() {
		for (ShadowConjurationFacts denied : new ShadowConjurationFacts[] {
				facts(1, true, false).withArtifact(true), facts(1, true, false).withAdminOnly(true),
				facts(1, true, false).withSpawnEgg(true), facts(1, true, false).withCrystal(true, false),
				facts(1, false, false), facts(1, false, true)}) {
			assertFalse(ShadowConjurationRules.evaluate(denied).allowed());
		}
	}

	@Test
	void darkCrystalIsTheOnlyCrystalAndRequiresTheFullRite() {
		var low = ShadowConjurationRules.evaluate(facts(1, true, false)
				.withCrystal(true, true).withEnergy(1849));
		assertFalse(low.allowed());
		assertEquals("full_energy_required", low.reason());
		var ready = ShadowConjurationRules.evaluate(facts(1, true, false)
				.withCrystal(true, true).withEnergy(1850));
		assertTrue(ready.allowed());
		assertTrue(ready.rite());
		assertEquals(1850, ready.cost());
		assertEquals(1200, ready.channelTicks());
	}

	private static ShadowConjurationFacts facts(int count, boolean ownNamespace, boolean testing) {
		return new ShadowConjurationFacts(count, 64, ShadowConjurationTier.COMMON, ownNamespace,
				false, false, false, false, false, false, testing, 1850);
	}

	private static void assertDecision(ShadowConjurationTier tier, int cost,
			ShadowConjurationFacts facts) {
		var decision = ShadowConjurationRules.evaluate(facts);
		assertTrue(decision.allowed());
		assertEquals(tier, decision.tier());
		assertEquals(cost, decision.cost());
	}
}
