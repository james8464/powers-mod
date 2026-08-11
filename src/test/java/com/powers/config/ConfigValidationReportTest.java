package com.powers.config;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigValidationReportTest {
	@Test
	void parseReportsDefaultSubstitutionAndClampWithoutRawValues() {
		PowersConfigLoader.ParseResult parsed = PowersConfigLoader.parseWithReport("""
				{
				  "schemaVersion": 3,
				  "wardRadius": 999,
				  "maxParticlesPerTick": "not-a-number",
				  "safeZones": "not-an-array",
				  "dialogueProvider": {"timeoutMillis": 1}
				}
				""");

		assertEquals(64, parsed.config().wardRadius());
		assertEquals(512, parsed.config().maxParticlesPerTick());
		assertEquals(250, parsed.config().dialogueProvider().timeoutMillis());
		assertTrue(parsed.report().entries().contains(new ConfigValidationReport.Entry(
				"wardRadius", ConfigValidationReport.Kind.CLAMPED)));
		assertTrue(parsed.report().entries().contains(new ConfigValidationReport.Entry(
				"maxParticlesPerTick", ConfigValidationReport.Kind.DEFAULTED)));
		assertTrue(parsed.report().entries().contains(new ConfigValidationReport.Entry(
				"safeZones", ConfigValidationReport.Kind.DEFAULTED)));
		assertTrue(parsed.report().entries().contains(new ConfigValidationReport.Entry(
				"dialogueProvider.timeoutMillis", ConfigValidationReport.Kind.CLAMPED)));
		assertTrue(parsed.report().summary().matches("revision=\\d+; adjustments=\\d+; retained=\\d+; dropped=\\d+"));
	}

	@Test
	void reportRetainsOnlyABoundedNumberOfFieldReasons() {
		List<ConfigValidationReport.Entry> entries = new ArrayList<>();
		for (int index = 0; index < 100; index++) {
			entries.add(new ConfigValidationReport.Entry("field." + index,
					ConfigValidationReport.Kind.DEFAULTED));
		}

		ConfigValidationReport report = ConfigValidationReport.of(7, entries);

		assertEquals(100, report.adjustments());
		assertEquals(ConfigValidationReport.MAX_ENTRIES, report.entries().size());
		assertEquals(100 - ConfigValidationReport.MAX_ENTRIES, report.dropped());
	}
}
