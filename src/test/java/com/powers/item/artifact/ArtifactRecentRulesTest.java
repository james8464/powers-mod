package com.powers.item.artifact;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ArtifactRecentRulesTest {
	@Test
	void recordingMovesCanonicalKeyToFrontDeduplicatesAndCapsHistory() {
		List<String> recent = List.of("a", "b", "c", "d", "e", "f", "g", "h");
		assertEquals(List.of("c", "a", "b", "d", "e", "f", "g", "h"),
				ArtifactRecentRules.record(recent, "c"));
		assertEquals(List.of("i", "a", "b", "c", "d", "e", "f", "g"),
				ArtifactRecentRules.record(recent, "i"));
	}

	@Test
	void reconciliationDropsUnknownAndMalformedKeysWithoutChangingOrder() {
		assertEquals(List.of("innate/fireball", "unique/blight_ground"),
				ArtifactRecentRules.reconcile(
						List.of("missing", "innate/fireball", "innate/fireball", "", "unique/blight_ground"),
						List.of("unique/blight_ground", "innate/fireball")));
	}
}
