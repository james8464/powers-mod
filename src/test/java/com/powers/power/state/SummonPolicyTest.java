package com.powers.power.state;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SummonPolicyTest {
	@Test
	void ephemeralSummonsAreNeverWrittenToTheWorldSave() {
		assertFalse(SummonPolicy.shouldPersist(true));
		assertTrue(SummonPolicy.shouldPersist(false));
	}
}
