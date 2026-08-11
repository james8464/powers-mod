package com.powers.companion;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShadowDiagnosticsTest {
	@Test
	void reportIsBoundedAndNeverContainsConversationContent() {
		ShadowDiagnostics diagnostics = new ShadowDiagnostics(2, 1, 1, 1, 2500,
				1, 3, 42, 7, 2, 12, 4, 1, 0, 0);
		String summary = diagnostics.summary();
		assertTrue(summary.contains("contexts=12"));
		assertTrue(summary.contains("leaks=0"));
		assertFalse(summary.toLowerCase().contains("conversation"));
		assertFalse(summary.toLowerCase().contains("message"));
	}
}
