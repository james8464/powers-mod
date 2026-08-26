package com.powers.fx;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
		assertTrue(cast.indexOf("boolean activated = transaction.committed();")
				< cast.indexOf(HOOK), "Ordinary/input emission preceded transaction commit");
		assertTrue(cast.indexOf("if (source == CastSource.INNATE)")
				< cast.indexOf(HOOK), "Ordinary/input emission was not source-gated");
		assertTrue(toggle.indexOf("if (activated) {") < toggle.indexOf(HOOK),
				"Toggle-on emission was outside the committed branch");
		assertTrue(toggle.indexOf("if (source == CastSource.INNATE)")
				< toggle.indexOf(HOOK), "Toggle-on emission was not source-gated");
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
}
