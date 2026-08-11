package com.powers.quality;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class LongRunHarnessContractTest {
	@Test
	void restartSoakIsIsolatedPersistentAndDefaultsToTwentyFourHours() throws IOException {
		String script = Files.readString(Path.of("scripts/restart_soak.py"));
		assertTrue(script.contains("default=24.0"));
		assertTrue(script.contains("powersRunDir"));
		assertTrue(script.contains("save-all flush"));
		assertTrue(script.contains("powers diagnose"));
		assertTrue(script.contains("restart-soak-report.json"));
		assertTrue(script.contains("forcedChunks=0"));
		assertTrue(script.contains("start_new_session=True"));
		assertTrue(script.contains("os.killpg"));
	}

	@Test
	void connectedBotProfilesAreManualThirtyMinuteJfrRuns() throws IOException {
		String source = Files.readString(Path.of(
				"src/gametest/java/com/powers/gametest/ConnectedBotProfileGameTests.java"));
		String profiler = Files.readString(Path.of(
				"src/main/java/com/powers/performance/ServerTickProfiler.java"));
		assertTrue(source.contains("manualOnly = true"));
		assertTrue(source.contains("PROFILE_TICKS = 36_000"));
		assertTrue(profiler.contains("new Recording()"));
		assertTrue(source.contains("List.of(10, 50, 100)"));
		assertTrue(profiler.contains("p95Mspt"));
		assertTrue(profiler.contains("p99Mspt"));
	}
}
