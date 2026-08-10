package com.powers.player;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerGuideTest {
	@Test
	void authoredGuideContainsCompleteControlsAndSafetyCopy() {
		assertEquals("POWERS: First Awakening", PlayerGuide.title());
		assertTrue(PlayerGuide.pages().size() >= 5);
		String joined = PlayerGuide.pages().stream()
				.map(page -> page.raw().getString()).collect(java.util.stream.Collectors.joining(" "));
		assertTrue(joined.contains("V, X, C"));
		assertTrue(joined.contains("Shadow"));
		assertTrue(joined.contains("vulnerable"));
	}
}
