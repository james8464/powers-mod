package com.powers.quality;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Proves the checked-in launcher can find Java 25 without machine-specific setup. */
class LauncherContractTest {
	private static final Path LAUNCHER = Path.of("test.sh");

	@Test
	void explicitJavaHomeWorksFromOutsideTheRepository() throws Exception {
		Path project = Path.of(System.getProperty("user.dir"));
		ProcessBuilder builder = new ProcessBuilder(project.resolve(LAUNCHER).toString(), "doctor")
				.directory(project.getParent().toFile()).redirectErrorStream(true);
		builder.environment().put("POWERS_JAVA_HOME", System.getProperty("java.home"));
		Process process = builder.start();
		assertTrue(process.waitFor(15, TimeUnit.SECONDS), "launcher doctor timed out");
		String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
		assertEquals(0, process.exitValue(), output);
		assertTrue(output.contains("Java 25"), output);
		assertTrue(output.contains(project.toString()), output);
	}

	@Test
	void launcherExposesEverySupportedVerificationMode() throws IOException {
		String source = Files.readString(Path.of(System.getProperty("user.dir")).resolve(LAUNCHER));
		for (String mode : java.util.List.of("client", "server", "check", "gametest", "soak", "doctor")) {
			assertTrue(source.contains(mode + ")"), () -> "missing launcher mode: " + mode);
		}
		assertTrue(source.contains("POWERS_JAVA_HOME"), "explicit Java override is missing");
		assertFalse(source.contains("JAVA_HOME:-/opt/homebrew"),
				"launcher still depends on one Homebrew installation");
	}

	@Test
	void gameTestTaskSeedsItsOwnRuntimeFiles() throws IOException {
		String build = Files.readString(Path.of(System.getProperty("user.dir")).resolve("build.gradle"));
		assertTrue(build.contains("tasks.named(\"runGameTest\")"),
				"runGameTest does not configure its isolated working directory");
		assertTrue(build.contains("server.properties") && build.contains("eula.txt"),
				"GameTest runtime files are not seeded before server bootstrap");
	}
}
