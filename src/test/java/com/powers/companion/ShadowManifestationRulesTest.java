package com.powers.companion;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShadowManifestationRulesTest {
	private static final UUID RECORDED = UUID.fromString("00000000-0000-0000-0000-000000000010");
	private static final UUID OTHER = UUID.fromString("00000000-0000-0000-0000-000000000020");

	@Test
	void revealTransitionsReuseTheBodyAndNeverCleanseItsState() {
		var transition = ShadowManifestationRules.visibility(RECORDED, false, true);
		assertEquals(RECORDED, transition.bodyId());
		assertTrue(transition.revealed());
		assertFalse(transition.replaceBody());
		assertFalse(transition.restoreHealthOrEffects());
	}

	@Test
	void recordedBodyWinsDuplicateReconciliationAndDeathCreatesARecallDelay() {
		assertEquals(RECORDED,
				ShadowManifestationRules.canonicalBody(RECORDED, List.of(OTHER, RECORDED)).orElseThrow());
		assertEquals(OTHER,
				ShadowManifestationRules.canonicalBody(null, List.of(OTHER)).orElseThrow());

		var dead = ShadowManifestationRules.afterDeath(
				ShadowCompanionData.defaults().withBodyId(RECORDED), 1_000L);
		assertTrue(dead.bodyUuid().isEmpty());
		assertEquals(ShadowStance.DOWNED, dead.stance());
		assertEquals(ShadowCompanionRules.recallEnergy(), dead.energy());
		assertFalse(ShadowManifestationRules.mayRecall(dead, dead.recallReadyAt() - 1));
		assertTrue(ShadowManifestationRules.mayRecall(dead, dead.recallReadyAt()));
	}
}
