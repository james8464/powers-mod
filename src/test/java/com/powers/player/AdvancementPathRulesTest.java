package com.powers.player;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
