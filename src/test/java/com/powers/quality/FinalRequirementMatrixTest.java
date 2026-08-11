package com.powers.quality;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Keeps finalisation status explicit without forcing documentation to overclaim proof. */
class FinalRequirementMatrixTest {
	private static final Path MATRIX = Path.of("docs/verification/final-requirement-matrix.md");

	@Test
	void everyTrackedRequirementHasAnExplicitStatusAndEvidence() throws IOException {
		List<String> rows = Files.readAllLines(MATRIX).stream()
				.filter(line -> line.matches("\\| R\\d{2} .*"))
				.toList();
		assertTrue(rows.size() >= 30, "the final matrix must cover every major request group");
		assertTrue(rows.stream().allMatch(line -> line.matches(
				"\\| R\\d{2} \\| [^|]+ \\| (?:Complete|Partial) \\| [^|]+ \\|")),
				"every matrix row needs a supported status and non-empty evidence");
		assertTrue(rows.stream().filter(line -> line.contains("| Complete |"))
				.noneMatch(line -> line.toLowerCase().matches(".*(?:remains open|not yet|outstanding).*")),
				"a completed row cannot describe known outstanding work");
		assertTrue(rows.stream().allMatch(line -> line.contains("| Complete |")),
				"the release matrix cannot retain implementation work after the final gate");

		String matrix = String.join("\n", rows).toLowerCase();
		for (String required : List.of("energy hud", "shadow sword", "realm", "grimoire",
				"collision", "performance", "first vessel", "documentation", "game tests")) {
			assertTrue(matrix.contains(required), "missing tracked evidence for " + required);
		}
	}
}
