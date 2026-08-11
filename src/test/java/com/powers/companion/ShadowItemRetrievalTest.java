package com.powers.companion;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShadowItemRetrievalTest {
	@Test
	void searchBudgetsAreHardCapped() {
		assertEquals(32.0, ShadowItemRetrieval.RADIUS);
		assertEquals(64, ShadowItemRetrieval.MAX_CANDIDATES);
		assertEquals(200, ShadowItemRetrieval.MAX_TASK_TICKS);
	}
}
