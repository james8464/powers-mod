package com.powers.knowledge;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KnowledgeIndexTest {
	private static KnowledgeEntry entry(String id, String title, List<String> keywords,
			String answer, int revealRank) {
		return new KnowledgeEntry(id, title, keywords, answer,
				List.of("powers:knowledge_entries/" + id), revealRank);
	}

	@Test
	void exactTitleAndKeywordMatchesOutrankLooseTokenOverlap() {
		KnowledgeIndex index = new KnowledgeIndex(List.of(
				entry("darkness", "Living Darkness", List.of("darkness block", "spread"),
						"Darkness spreads through vulnerable terrain.", 0),
				entry("sword", "Shadow Sword", List.of("darkness weapon", "artifact"),
						"The sword routes invocations.", 0)));

		KnowledgeAnswer answer = index.answer("How does a darkness block spread?", 0);
		assertEquals("darkness", answer.entryId());
		assertTrue(answer.confidence() >= 0.7);
	}

	@Test
	void progressionSpoilersAreFilteredUntilRevealRank() {
		KnowledgeIndex index = new KnowledgeIndex(List.of(
				entry("herald", "The Hidden Herald", List.of("boss herald"), "A final herald waits.", 8)));

		assertTrue(index.answer("Where is the hidden herald?", 4).answer().contains("cannot yet"));
		assertEquals("herald", index.answer("Where is the hidden herald?", 8).entryId());
	}

	@Test
	void noMatchAdmitsUncertaintyInsteadOfInventingARecipe() {
		KnowledgeIndex index = new KnowledgeIndex(List.of());
		KnowledgeAnswer answer = index.answer("craft a crystal", 10);
		assertEquals(0.0, answer.confidence());
		assertTrue(answer.answer().contains("cannot verify"));
	}
}
