package com.powers.spell;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BloodReadingReportTest {
	@Test
	void reportBoundsEffectsAndCalculatesHealthPercentage() {
		BloodReadingReport report = BloodReadingReport.bounded(30.0F, 120.0F, 18.0,
				true, false, List.of("a", "b", "c", "d", "e", "f", "g", "h", "i"));
		assertEquals(25, report.healthPercent());
		assertEquals(8, report.effectIds().size());
		assertEquals(BloodReadingReport.Alignment.DARKNESS, report.alignment());
	}

	@Test
	void invalidNumbersAreSanitizedRatherThanLeakingToChat() {
		BloodReadingReport report = BloodReadingReport.bounded(Float.NaN, -2.0F,
				Double.POSITIVE_INFINITY, false, true, List.of());
		assertEquals(0, report.healthPercent());
		assertEquals(0.0F, report.health());
		assertEquals(BloodReadingReport.Alignment.AMETHYST_DAMPENED, report.alignment());
		assertTrue(report.effectIds().isEmpty());
	}
}
