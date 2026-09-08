package com.powers.boss;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BossReactionRulesTest {
	@Test
	void heraldsReactOnlyToTheirOpposition() {
		assertEquals(BossReactionRules.Reaction.OPPOSITION_STAGGER,
				BossReactionRules.reaction(BossReactionRules.BossKind.LIGHT_HERALD,
						BossReactionRules.Stimulus.DARK_ARTIFACT));
		assertEquals(BossReactionRules.Reaction.OPPOSITION_STAGGER,
				BossReactionRules.reaction(BossReactionRules.BossKind.LIGHT_HERALD,
						BossReactionRules.Stimulus.UMBRAL_CONCORD));
		assertEquals(BossReactionRules.Reaction.NONE,
				BossReactionRules.reaction(BossReactionRules.BossKind.LIGHT_HERALD,
						BossReactionRules.Stimulus.LIGHT_ARTIFACT));
		assertEquals(BossReactionRules.Reaction.OPPOSITION_STAGGER,
				BossReactionRules.reaction(BossReactionRules.BossKind.DARK_HERALD,
						BossReactionRules.Stimulus.LIGHT_ARTIFACT));
		assertEquals(BossReactionRules.Reaction.NONE,
				BossReactionRules.reaction(BossReactionRules.BossKind.DARK_HERALD,
						BossReactionRules.Stimulus.DARK_ARTIFACT));
	}

	@Test
	void firstVesselAcceptsEitherAlignmentAndCooldownsStayFinite() {
		for (BossReactionRules.Stimulus stimulus : BossReactionRules.Stimulus.values()) {
			assertEquals(BossReactionRules.Reaction.COVENANT_BREACH,
					BossReactionRules.reaction(BossReactionRules.BossKind.FIRST_VESSEL,
							stimulus));
		}
		assertTrue(BossReactionRules.offCooldown(200L, 100L));
		assertFalse(BossReactionRules.offCooldown(159L, 100L));
		assertTrue(BossReactionRules.reactionDurationTicks(
				BossReactionRules.Reaction.COVENANT_BREACH) <= 200);
	}
}
