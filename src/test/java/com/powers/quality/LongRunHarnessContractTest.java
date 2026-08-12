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
		String build = Files.readString(Path.of("build.gradle"));
		String metadata = Files.readString(Path.of("src/gametest/resources/fabric.mod.json"));
		assertTrue(build.contains("powersConnectedProfile"));
		assertTrue(build.contains("powersProfileTicks"));
		assertTrue(build.contains("connected_bot_profile_game_tests_connected_ten_fifty_and_hundred_player_profiles"));
		assertTrue(metadata.contains("${connectedProfileEntrypoint}"));
		assertTrue(source.contains("manualOnly = false"));
		assertTrue(source.contains("PROFILE_TICKS = 36_000"));
		assertTrue(source.contains("helper.onEachTick"));
		assertTrue(source.contains("AbilityActivationService.activate"));
		assertTrue(source.contains("TestingOverrides.setAll"));
		assertTrue(source.contains("PROFILE_ACTIONS"));
		assertTrue(source.contains("true"));
		assertTrue(profiler.contains("new Recording()"));
		assertTrue(profiler.contains("wall_seconds"));
		assertTrue(profiler.contains("attempted_actions"));
		assertTrue(profiler.contains("successful_actions"));
		assertTrue(source.contains("List.of(10, 50, 100)"));
		assertTrue(profiler.contains("p95Mspt"));
		assertTrue(profiler.contains("p99Mspt"));
	}
}
