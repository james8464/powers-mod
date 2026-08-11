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

	@Test
	void formattingCompatibilityAndConfusablesCannotForgeUniqueness() {
		UniqueNameIndex<String> index = new UniqueNameIndex<>();
		index.upsert("plain", "Watcher");
		index.upsert("formatted", "§5Ｗatcher\u200B");
		index.upsert("confusable", "Wаtcher"); // Cyrillic small a.

		assertEquals(List.of("confusable", "formatted"), index.candidates("watcher", 2));
	}

	@Test
	void controlOnlyAndOversizedNamesAreNotIndexed() {
		UniqueNameIndex<String> index = new UniqueNameIndex<>();
		index.upsert("control", "\u0000\u200B§5");
		index.upsert("huge", "x".repeat(257));
		assertTrue(index.candidates("", 2).isEmpty());
		assertTrue(index.candidates("x".repeat(257), 2).isEmpty());
	}

	@Test
	void diagnosticsExposeCandidateAndStaleWork() {
		UniqueNameIndex<String> index = new UniqueNameIndex<>();
		index.upsert("one", "Watcher");
		assertEquals(List.of("one"), index.candidates("watcher", 2));
		assertTrue(index.candidates("missing", 2).isEmpty());
		index.removeStale("one");

		var diagnostics = index.diagnostics();
		assertEquals(2, diagnostics.queries());
		assertEquals(1, diagnostics.candidates());
		assertEquals(1, diagnostics.misses());
		assertEquals(1, diagnostics.staleRemovals());
		assertEquals(0, diagnostics.entries());
		assertTrue(diagnostics.estimatedBytes() >= 0);
	}
}
