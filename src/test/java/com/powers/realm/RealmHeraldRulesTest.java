package com.powers.realm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RealmHeraldRulesTest {
	@Test
	void eachHeraldDefendsItsForceAndRespawnsOnlyAfterTwentyMinutes() {
		assertFalse(RealmHeraldRules.mayTarget(RealmKind.DARK, true));
		assertTrue(RealmHeraldRules.mayTarget(RealmKind.DARK, false));
		assertTrue(RealmHeraldRules.mayTarget(RealmKind.LIGHT, true));
		assertFalse(RealmHeraldRules.mayTarget(RealmKind.LIGHT, false));
		long defeatedAt = 40_000L;
		long next = RealmHeraldRules.nextSpawnTime(defeatedAt);
		assertFalse(RealmHeraldRules.maySpawn(next - 1, next));
		assertTrue(RealmHeraldRules.maySpawn(next, next));
	}
}
