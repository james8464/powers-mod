package com.powers.testing;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ManualAcceptanceChecklistTest {
	@BeforeAll
	static void bootstrapRegistries() {
		net.minecraft.SharedConstants.tryDetectVersion();
		net.minecraft.server.Bootstrap.bootStrap();
		com.powers.power.PowerRegistry.initialize();
	}

	@Test
	void registryPresenceCannotBeReportedAsBehavioralPass() {
		assertEquals("MANUAL LIVE PENDING",
				ManualAcceptanceChecklistReport.statusFor(GameplayAcceptanceCatalogue.Proof.LIVE_REGISTRY));
		assertEquals("AUTOMATED BEHAVIOR PASS",
				ManualAcceptanceChecklistReport.statusFor(GameplayAcceptanceCatalogue.Proof.LIVE_BEHAVIOR));
		assertEquals("AUTOMATED RULE PASS",
				ManualAcceptanceChecklistReport.statusFor(GameplayAcceptanceCatalogue.Proof.UNIT_RULES));
		assertEquals("RESOURCE CONTRACT PASS",
				ManualAcceptanceChecklistReport.statusFor(GameplayAcceptanceCatalogue.Proof.RESOURCE));
		assertEquals("AUTOMATED SOAK PASS",
				ManualAcceptanceChecklistReport.statusFor(GameplayAcceptanceCatalogue.Proof.SOAK));
	}

	@Test
	void generatedChecklistContainsEveryRegisteredItemActionEntityScreenAndCommand() throws Exception {
		String text = Files.readString(Path.of("docs/verification/manual-acceptance-checklist.md"));
		for (GameplayAcceptanceCatalogue.Entry entry : GameplayAcceptanceCatalogue.entries()) {
			assertTrue(text.contains("`" + entry.id() + "`"), entry.id());
		}
		assertEquals(262, text.lines().filter(line -> line.startsWith("| item | `powers:")).count());
		assertEquals(23, text.lines().filter(line -> line.startsWith("| innate | ")).count());
		assertEquals(11, text.lines().filter(line -> line.startsWith("| screen | ")).count());
		assertEquals(32, text.lines().filter(line -> line.startsWith("| command | ")).count());
		assertFalse(text.lines().anyMatch(line -> line.startsWith("| innate | `size_shift`")
				&& line.contains("AUTOMATED PASS")));
		assertFalse(text.lines().anyMatch(line -> line.startsWith("| item | `powers:")
				&& line.contains("AUTOMATED PASS")));
	}
}
