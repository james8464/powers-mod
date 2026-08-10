package com.powers.diagnostics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuntimeDiagnosticSnapshotTest {
	@Test
	void summaryExposesEveryRequiredRuntimeFamily() {
		RuntimeDiagnosticSnapshot snapshot = new RuntimeDiagnosticSnapshot(
				3, 9, 4, 32, 2, 7, 120, 1, 4096, 512,
				2, 1, 1, 362, 18, 240, 88);

		assertEquals(4, snapshot.lines().size());
		String report = String.join("\n", snapshot.lines());
		assertTrue(report.contains("magic=3/9"));
		assertTrue(report.contains("spellFields=4 (cap/tick 32)"));
		assertTrue(report.contains("proxies=2"));
		assertTrue(report.contains("forcedChunks=362"));
		assertTrue(report.contains("packets=18"));
		assertTrue(report.contains("particles=240"));
		assertTrue(report.contains("entityInspections=88"));
	}
}
