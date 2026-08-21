package com.powers.item.artifact;

import org.junit.jupiter.api.Test;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArtifactRecentRulesTest {
	@Test
	void persistenceCodecRejectsOversizedHistoriesBeforeMaterializingGameplayState() {
		String json = java.util.stream.IntStream.range(0, 40)
				.mapToObj(index -> "\"innate/action_" + index + "\"")
				.collect(java.util.stream.Collectors.joining(",", "[", "]"));
		assertTrue(ArtifactRecentRules.CODEC.parse(JsonOps.INSTANCE,
				JsonParser.parseString(json)).error().isPresent());
		List<String> decoded = ArtifactRecentRules.CODEC.parse(JsonOps.INSTANCE,
				JsonParser.parseString("[\"innate/action_0\",\"innate/action_1\"]")).getOrThrow();
		assertEquals(List.of("innate/action_0", "innate/action_1"), decoded);
	}
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
