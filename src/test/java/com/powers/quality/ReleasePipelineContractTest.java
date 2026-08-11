package com.powers.quality;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ReleasePipelineContractTest {
	private static final Path ROOT = Path.of(System.getProperty("user.dir"));

	@Test
	void filteredRunsUseCallerSpecificResultDirectories() throws IOException {
		String build = Files.readString(ROOT.resolve("build.gradle"));
		assertTrue(build.contains("POWERS_TEST_RUN_ID"));
		assertTrue(build.contains("powersTestRunId"));
		assertTrue(build.contains("test-results/${testTask.name}/${testRunId}"));
		assertTrue(build.contains("reports/tests/${testTask.name}/${testRunId}"));
	}

	@Test
	void continuousIntegrationRunsEveryMandatoryReleaseLaneOnJava25() throws IOException {
		String workflow = Files.readString(ROOT.resolve(".github/workflows/ci.yml"));
		assertTrue(workflow.contains("actions/setup-java@v5"));
		assertTrue(workflow.contains("java-version: '25'"));
		for (String command : new String[] {
				"./gradlew clean check", "./test.sh gametest", "server_smoke.py",
				"saveMigrationCorpus", "pitest", "validatePowerResources",
				"verifyMagicDocs", "verifyItemDocs", "verifyRankDocs", "generateRankDocs",
				"verifyScreenshots",
				"./gradlew syntheticSoak", "git diff --exit-code"
		}) {
			assertTrue(workflow.contains(command), () -> "Missing CI lane: " + command);
		}
		assertTrue(workflow.contains("synthetic-soak:"));
		assertTrue(workflow.contains("POWERS_TEST_RUN_ID=synthetic-soak"));
	}

	@Test
	void syntheticSoakHasASeparatelyAttributedGradleTask() throws IOException {
		String build = Files.readString(ROOT.resolve("build.gradle"));
		assertTrue(build.contains("tasks.register(\"syntheticSoak\", Test)"));
		assertTrue(build.contains("com.powers.performance.SyntheticMultiplayerSoakTest"));
		assertTrue(build.contains("tasks.register(\"testPythonScripts\", Exec)"));
		assertTrue(build.contains("\"-m\", \"unittest\""));
		assertTrue(build.contains("tasks.register(\"generateRankDocs\", Exec)"));
		assertTrue(build.contains("tasks.register(\"verifyRankDocs\", Exec)"));
	}

	@Test
	void criticalPureRulesHaveAnEnforcedMutationThreshold() throws IOException {
		String build = Files.readString(ROOT.resolve("build.gradle"));
		assertTrue(build.contains("info.solidsoft.pitest"));
		assertTrue(build.contains("mutationThreshold = 80"));
		assertTrue(build.contains("coverageThreshold = 85"));
		assertTrue(build.contains("MagicInteractionResolver"));
		assertTrue(build.contains("ArtifactAuthorizationRules"));
	}
}
