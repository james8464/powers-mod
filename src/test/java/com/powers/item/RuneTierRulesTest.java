package com.powers.item;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RuneTierRulesTest {
	@Test
	void authoredRunestoneSizesCarryDistinctEnergy() {
		assertEquals(40, RuneTierRules.energyFor("artifact_runestone_inert"));
		assertEquals(60, RuneTierRules.energyFor("artifact_runestone_dark_tiny"));
		assertEquals(125, RuneTierRules.energyFor("artifact_runestone_dark_small"));
		assertEquals(250, RuneTierRules.energyFor("artifact_runestone_dark_medium"));
		assertEquals(400, RuneTierRules.energyFor("artifact_runestone_dark_large"));
		assertEquals(600, RuneTierRules.energyFor("artifact_runestone_dark_inscribed_large"));
	}

	@Test
	void largerRunesHaveLongerButBoundedChannelRecovery() {
		assertEquals(70, RuneTierRules.cooldownTicks(40));
		assertEquals(210, RuneTierRules.cooldownTicks(600));
	}
}
