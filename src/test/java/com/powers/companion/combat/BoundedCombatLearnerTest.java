package com.powers.companion.combat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoundedCombatLearnerTest {
	@Test
	void rewardModifierExplorationAndCountsAreStrictlyBounded() {
		ShadowLearningState state = new ShadowLearningState();
		for (int i = 0; i < 500; i++) state.adjust("context", "boss", "void_beam", 99);
		assertEquals(.25, state.modifier("context", "boss", "void_beam"), 1.0E-9);
		for (int i = 0; i < 80; i++) state.adjust("c" + i, "t" + i, "fireball", -99);
		assertEquals(64, state.contextCount());
		assertEquals(32, state.typeCount());
		assertFalse(BoundedCombatLearner.shouldExplore(.01, true, 0));
		assertFalse(BoundedCombatLearner.shouldExplore(.01, false, 1));
		assertTrue(BoundedCombatLearner.shouldExplore(.01, false, 0));
		assertFalse(BoundedCombatLearner.shouldExplore(.051, false, 0));
	}

	@Test
	void creditCompletionAndVersionedPersistenceAreDeterministic() {
		ShadowLearningState state = new ShadowLearningState();
		BoundedCombatLearner learner = new BoundedCombatLearner(state);
		assertTrue(learner.openCredit("ctx", "boss", "starfall", 10, .8, .9, .7));
		assertFalse(learner.openCredit("other", "boss", "fireball", 11, 1, 1, 1));
		double reward = learner.completeCredit(20, .3, .8, .7);
		assertTrue(reward > 0);
		String encoded = state.encode();
		ShadowLearningState decoded = ShadowLearningState.decode(encoded);
		assertEquals(state.modifier("ctx", "boss", "starfall"),
				decoded.modifier("ctx", "boss", "starfall"), 1.0E-6);
	}
}
