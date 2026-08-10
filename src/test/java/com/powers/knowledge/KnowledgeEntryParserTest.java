package com.powers.knowledge;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class KnowledgeEntryParserTest {
	@Test
	void parsesBoundedDatapackEntryAndAddsResourceSource() {
		KnowledgeEntry entry = KnowledgeEntryParser.parse("powers:darkness", JsonParser.parseString("""
				{
				  "title": "Living Darkness",
				  "keywords": ["darkness block", "spread"],
				  "answer": "It spreads over time.",
				  "sources": ["README: Living Forces"],
				  "reveal_rank": 2
				}
				""").getAsJsonObject());

		assertEquals("powers:darkness", entry.id());
		assertEquals(2, entry.revealRank());
		assertEquals("data/powers/knowledge_entries/darkness.json", entry.sources().getLast());
	}

	@Test
	void rejectsOversizedAnswersAndMalformedKeywords() {
		assertThrows(IllegalArgumentException.class, () -> KnowledgeEntryParser.parse("powers:bad",
				JsonParser.parseString("{\"title\":\"Bad\",\"keywords\":{},\"answer\":\"x\"}")
						.getAsJsonObject()));
		assertThrows(IllegalArgumentException.class, () -> KnowledgeEntryParser.parse("powers:bad",
				JsonParser.parseString("{\"title\":\"Bad\",\"answer\":\"" + "x".repeat(5000)
						+ "\"}").getAsJsonObject()));
	}
}
