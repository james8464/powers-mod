package com.powers.mind;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Proves missing-dimension reports stay bounded while preserving actionable counts. */
class PersistentDimensionDiagnosticsTest {
	@AfterEach
	void clear() {
		PersistentDimensionDiagnostics.clear();
	}

	@Test
	void coalescesRepeatedFailuresAndBoundsDistinctKeys() {
		PersistentDimensionDiagnostics.record("body", "removed:realm");
		PersistentDimensionDiagnostics.record("body", "removed:realm");
		for (int index = 0; index < 200; index++) {
			PersistentDimensionDiagnostics.record("portal", "removed:" + index);
		}

		var snapshot = PersistentDimensionDiagnostics.snapshot();
		assertEquals(128, snapshot.issues().size());
		assertTrue(snapshot.droppedDistinctKeys() > 0);
		assertEquals(2, snapshot.issues().stream()
				.filter(issue -> issue.feature().equals("body"))
				.findFirst().orElseThrow().occurrences());
	}

	@Test
	void invalidInputCannotPolluteOperatorOutput() {
		PersistentDimensionDiagnostics.record("", "powers:realm");
		PersistentDimensionDiagnostics.record("body", "\nforged-log-line");
		assertTrue(PersistentDimensionDiagnostics.snapshot().issues().isEmpty());
	}
}
