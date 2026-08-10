package com.powers.network;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UniqueNameIndexTest {
	@Test
	void renameMovesAnIdentityBetweenNormalizedBuckets() {
		UniqueNameIndex<String> index = new UniqueNameIndex<>();
		index.upsert("mob-1", "  Watcher  ");
		assertEquals(List.of("mob-1"), index.candidates("watcher", 2));
		index.upsert("mob-1", "Sentinel");
		assertTrue(index.candidates("watcher", 2).isEmpty());
		assertEquals(List.of("mob-1"), index.candidates("SENTINEL", 2));
	}

	@Test
	void candidateReadsStopAtTheAmbiguityThreshold() {
		UniqueNameIndex<String> index = new UniqueNameIndex<>();
		index.upsert("c", "Eye");
		index.upsert("a", "eye");
		index.upsert("b", "EYE");
		assertEquals(List.of("a", "b"), index.candidates("eye", 2));
		index.remove("a");
		assertEquals(List.of("b", "c"), index.candidates("eye", 2));
	}
}
