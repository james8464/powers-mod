package com.powers.player;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

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

	@Test
	void bindingDiagramUsesCurrentSelectionsWithoutBecomingASecondManual() {
		String diagram = PlayerGuide.bindingDiagram(List.of("fireball", "flight", "teleport"),
				List.of("blood_reading"), List.of("red_convergence"),
				List.of("nightfall_dominion", "void_beam"));
		assertTrue(diagram.contains("V → Fireball"));
		assertTrue(diagram.contains("Spell → Blood Reading"));
		assertTrue(diagram.contains("Crystal → Red Convergence"));
		assertTrue(diagram.contains("Wheel 1 → Nightfall Dominion"));
		assertTrue(diagram.length() < 700);
	}
}
