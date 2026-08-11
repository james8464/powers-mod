package com.powers.player;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdvancementPathRulesTest {
	@Test
	void normalPlayersSeeOnlyTheirRadiantPath() {
		AdvancementPathRules.Selection selection = AdvancementPathRules.select(false, 7, 4);

		assertEquals("skill_root", selection.activeRoot());
		assertEquals("darkness_root", selection.hiddenRoot());
		assertEquals(7, selection.reachedLevel());
	}

	@Test
	void darknessPlayersSeeOnlyTheirShadowPath() {
		AdvancementPathRules.Selection selection = AdvancementPathRules.select(true, 7, 4);

		assertEquals("darkness_root", selection.activeRoot());
		assertEquals("skill_root", selection.hiddenRoot());
		assertEquals(4, selection.reachedLevel());
	}

	@Test
	void journalSynchronizationRunsOnlyWhenAlignmentOrVisibleDepthChanges() {
		AdvancementPathRules.Selection lightSeven = AdvancementPathRules.select(false, 7, 4);

		assertTrue(AdvancementPathRules.needsSynchronization(null, lightSeven));
		assertFalse(AdvancementPathRules.needsSynchronization(lightSeven,
				AdvancementPathRules.select(false, 7, 10)));
		assertTrue(AdvancementPathRules.needsSynchronization(lightSeven,
				AdvancementPathRules.select(false, 8, 4)));
		assertTrue(AdvancementPathRules.needsSynchronization(lightSeven,
				AdvancementPathRules.select(true, 7, 4)));
	}
}
