package com.powers.quality;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Keeps the final playtest promises explicit and prevents silent regression to pending work. */
class FinalRequirementMatrixTest {
	private static final Path MATRIX = Path.of("docs/verification/final-requirement-matrix.md");

	@Test
	void everyTrackedRequirementHasCompletedEvidence() throws IOException {
		List<String> rows = Files.readAllLines(MATRIX).stream()
				.filter(line -> line.matches("\\| R\\d{2} .*"))
				.toList();
		assertTrue(rows.size() >= 30, "the final matrix must cover every major request group");
		assertTrue(rows.stream().allMatch(line -> line.contains("| Complete |")),
				"the final matrix must not hide pending or partial work");

		String matrix = String.join("\n", rows).toLowerCase();
		for (String required : List.of("energy hud", "shadow sword", "realm", "grimoire",
				"collision", "performance", "first vessel", "documentation", "game tests")) {
			assertTrue(matrix.contains(required), "missing tracked evidence for " + required);
		}
	}
}
