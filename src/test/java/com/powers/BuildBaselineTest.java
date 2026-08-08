package com.powers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class BuildBaselineTest {
	@Test
	void exposesTheRegisteredModNamespace() {
		assertEquals("powers", PowersMod.MOD_ID);
	}
}
