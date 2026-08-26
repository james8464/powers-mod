package com.powers.fx;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Guards the two committed innate-success boundaries against route drift or duplicate emission. */
class RankTenSilhouetteIntegrationSourceTest {
	private static final String HOOK =
			"RankTenSilhouetteService.afterSuccessfulInnateCast(player, ability.id().getPath());";

	@Test
	void exactlyTwoCommittedInnateBranchesOwnSilhouetteEmission() throws IOException {
		String source = Files.readString(Path.of(
				"src/main/java/com/powers/power/AbilityActivationService.java"));
		String cast = method(source, "private static Result cast(",
				"private static boolean passesCasterChecks(");
		String toggle = method(source, "private static Result toggle(",
				"private static String seconds(");

		assertEquals(2, occurrences(source, HOOK),
				"Only committed ordinary/input and toggle-on innate branches may emit");
		String failedCast = block(cast, "if (!activated) {");
		String committedCast = elseBlock(cast, "if (!activated) {");
		String committedCastInnate = block(committedCast, "if (source == CastSource.INNATE) {");
		assertFalse(failedCast.contains(HOOK), "Failed ordinary/input branch owned emission");
		assertTrue(committedCastInnate.contains(HOOK),
				"Ordinary/input emission was not lexically contained by commit and innate guards");

		String committedToggle = block(toggle, "if (activated) {");
		String committedToggleInnate = block(committedToggle,
				"if (source == CastSource.INNATE) {");
		assertTrue(committedToggleInnate.contains(HOOK),
				"Toggle emission was not lexically contained by commit and innate guards");
	}

	private static String method(String source, String start, String end) {
		int from = source.indexOf(start);
		int to = source.indexOf(end, from);
		assertTrue(from >= 0 && to > from, "Could not isolate activation method boundary");
		return source.substring(from, to);
	}

	private static int occurrences(String source, String needle) {
		int count = 0;
		for (int offset = 0; (offset = source.indexOf(needle, offset)) >= 0; offset += needle.length()) {
			count++;
		}
		return count;
	}

	private static String block(String source, String marker) {
		int markerAt = source.indexOf(marker);
		int open = markerAt < 0 ? -1 : source.indexOf('{', markerAt);
		assertTrue(open >= 0, "Could not find block for " + marker);
		int close = closingBrace(source, open, marker);
		return source.substring(open + 1, close);
	}

	private static String elseBlock(String source, String marker) {
		int markerAt = source.indexOf(marker);
		int open = markerAt < 0 ? -1 : source.indexOf('{', markerAt);
		assertTrue(open >= 0, "Could not find conditional for " + marker);
		int close = closingBrace(source, open, marker);
		int elseAt = source.indexOf("else", close + 1);
		int elseOpen = elseAt < 0 ? -1 : source.indexOf('{', elseAt);
		assertTrue(elseOpen >= 0, "Could not find else block for " + marker);
		return source.substring(elseOpen + 1, closingBrace(source, elseOpen, "else"));
	}

	private static int closingBrace(String source, int open, String marker) {
		int depth = 0;
		for (int index = open; index < source.length(); index++) {
			char character = source.charAt(index);
			if (character == '{') depth++;
			else if (character == '}' && --depth == 0) return index;
		}
		throw new AssertionError("Unclosed block for " + marker);
	}
}
