package com.powers.network;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class NamedTargetRulesTest {
	@Test
	void uniqueNamesResolveCaseInsensitivelyAcrossPlayerAndMobCandidates() {
		NamedTargetRules.Resolution<String> resolution = NamedTargetRules.resolve("  ember  ", List.of(
				new NamedTargetRules.Candidate<>("player", "Alex"),
				new NamedTargetRules.Candidate<>("mob", "Ember")));

		assertEquals(NamedTargetRules.Status.FOUND, resolution.status());
		assertEquals("mob", resolution.target());
	}

	@Test
	void duplicateCustomNamesRefuseToRevealEitherTarget() {
		NamedTargetRules.Resolution<String> resolution = NamedTargetRules.resolve("Watcher", List.of(
				new NamedTargetRules.Candidate<>("first", "Watcher"),
				new NamedTargetRules.Candidate<>("second", "watcher")));

		assertEquals(NamedTargetRules.Status.AMBIGUOUS, resolution.status());
		assertNull(resolution.target());
	}

	@Test
	void missingAndBlankNamesDoNotResolve() {
		assertEquals(NamedTargetRules.Status.NOT_FOUND,
				NamedTargetRules.resolve("missing", List.of(
						new NamedTargetRules.Candidate<>("player", "Alex"))).status());
		assertEquals(NamedTargetRules.Status.NOT_FOUND,
				NamedTargetRules.<String>resolve(" ", List.of()).status());
	}

	@Test
	void anIncompleteWorldScanCanNeverPretendANameIsUnique() {
		NamedTargetRules.Resolution<String> resolution = NamedTargetRules.scanLimit();

		assertEquals(NamedTargetRules.Status.SCAN_LIMIT, resolution.status());
		assertNull(resolution.target());
	}
}
