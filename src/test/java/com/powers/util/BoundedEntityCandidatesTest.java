package com.powers.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BoundedEntityCandidatesTest {
	@Test
	void batchReportsActualInspectionsAndProtectsItsSnapshot() {
		BoundedEntityCandidates.Batch<String> batch =
				new BoundedEntityCandidates.Batch<>(List.of("eligible"), 3);
		assertEquals(List.of("eligible"), batch.candidates());
		assertEquals(3, batch.inspected());
		assertThrows(UnsupportedOperationException.class,
				() -> batch.candidates().add("late mutation"));
	}
}
