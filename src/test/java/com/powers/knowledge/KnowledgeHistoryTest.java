package com.powers.knowledge;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KnowledgeHistoryTest {
	@Test
	void newestQuestionsAreBoundedAndNavigable() {
		KnowledgeHistory history = new KnowledgeHistory(3);
		history.record("one", new KnowledgeAnswer("1", "first", 1.0,
				java.util.List.of(), java.util.List.of()));
		history.record("two", new KnowledgeAnswer("2", "second", 1.0,
				java.util.List.of(), java.util.List.of()));
		history.record("three", new KnowledgeAnswer("3", "third", 1.0,
				java.util.List.of(), java.util.List.of()));
		history.record("four", new KnowledgeAnswer("4", "fourth", 1.0,
				java.util.List.of(), java.util.List.of()));

		assertEquals(3, history.entries().size());
		assertEquals("two", history.entries().getFirst().question());
		assertEquals("four", history.current().question());
		assertEquals("three", history.previous().question());
		assertEquals("four", history.next().question());
	}

	@Test
	void repeatedQuestionReplacesItsOldAnswer() {
		KnowledgeHistory history = new KnowledgeHistory(8);
		history.record("darkness", answer("old"));
		history.record("light", answer("light"));
		history.record("darkness", answer("new"));

		assertEquals(2, history.entries().size());
		assertEquals("new", history.current().answer().answer());
	}

	private static KnowledgeAnswer answer(String text) {
		return new KnowledgeAnswer(text, text, 0.8, java.util.List.of(), java.util.List.of());
	}
}
