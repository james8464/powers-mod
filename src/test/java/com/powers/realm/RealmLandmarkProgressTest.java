package com.powers.realm;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RealmLandmarkProgressTest {
	@Test
	void completionIsTrackedPerSiteAndDimension() {
		RealmLandmarkProgress progress = new RealmLandmarkProgress();
		assertEquals(List.of("one", "two"), progress.missing("light", List.of("one", "two")));
		assertTrue(progress.complete("light", "one"));
		assertFalse(progress.complete("light", "one"));
		assertEquals(List.of("two"), progress.missing("light", List.of("one", "two")));
		assertEquals(List.of("one", "two"), progress.missing("dark", List.of("one", "two")));
		progress.clear();
		assertEquals(0, progress.completedCount());
	}

	@Test
	void encodedSnapshotRoundTripsWorldPersistence() {
		RealmLandmarkProgress original = new RealmLandmarkProgress();
		original.complete("powers:light_realm", "light_memory_1");
		original.complete("powers:dark_realm", "dark_memory_2");

		RealmLandmarkProgress restored = new RealmLandmarkProgress(original.snapshot());
		assertEquals(List.of(), restored.missing("powers:light_realm", List.of("light_memory_1")));
		assertEquals(List.of(), restored.missing("powers:dark_realm", List.of("dark_memory_2")));
	}
}
