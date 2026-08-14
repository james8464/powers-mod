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
				  "dialogueProvider": {
				    "timeoutMillis": 1,
				    "credentialEnvironmentVariable": "secret-token-value"
				  }
				}
				""");

		assertEquals(64, parsed.config().wardRadius());
		assertEquals(512, parsed.config().maxParticlesPerTick());
		assertEquals(250, parsed.config().dialogueProvider().timeoutMillis());
		ConfigValidationReport.Entry ward = entry(parsed.report(), "wardRadius");
		assertEquals("999", ward.original());
		assertEquals("64", ward.sanitized());
		assertEquals("out_of_range", ward.reason());
		ConfigValidationReport.Entry particles = entry(parsed.report(), "maxParticlesPerTick");
		assertEquals("<invalid:string>", particles.original());
		assertEquals("512", particles.sanitized());
		assertEquals("invalid_type", particles.reason());
		ConfigValidationReport.Entry timeout = entry(parsed.report(), "dialogueProvider.timeoutMillis");
		assertEquals("1", timeout.original());
		assertEquals("250", timeout.sanitized());
		assertEquals("out_of_range", timeout.reason());
		String operatorReport = String.join("\n", parsed.report().operatorLines());
		assertTrue(operatorReport.contains("revision=" + parsed.report().revision()));
		assertTrue(operatorReport.contains("safeZones"));
		assertTrue(!operatorReport.contains("secret-token-value"));
		assertTrue(parsed.report().summary().matches("revision=\\d+; adjustments=\\d+; retained=\\d+; dropped=\\d+"));
	}

	@Test
	void nullSafeZoneEntriesDefaultSafelyWithoutCoordinatesInTheReport() {
		PowersConfigLoader.ParseResult parsed = PowersConfigLoader.parseWithReport(
				"{\"schemaVersion\":3,\"safeZones\":[null]}");

		assertTrue(parsed.config().safeZones().isEmpty());
		String report = parsed.report().entries().toString();
		assertTrue(report.contains("safeZones"));
		assertTrue(!report.contains("x=") && !report.contains("y=") && !report.contains("z="));
	}

	@Test
	void reportRetainsOnlyABoundedNumberOfFieldReasons() {
		List<ConfigValidationReport.Entry> entries = new ArrayList<>();
		for (int index = 0; index < 100; index++) {
			entries.add(new ConfigValidationReport.Entry("field." + index,
					ConfigValidationReport.Kind.DEFAULTED, "<missing>", "default", "missing"));
		}

		ConfigValidationReport report = ConfigValidationReport.of(7, entries);

		assertEquals(100, report.adjustments());
		assertEquals(ConfigValidationReport.MAX_ENTRIES, report.entries().size());
		assertEquals(100 - ConfigValidationReport.MAX_ENTRIES, report.dropped());
	}

	@Test
	void parsesWorldAndDimensionPolicyPatchesWithoutFlatteningTheirOrigins() {
		PowersConfigLoader.ParseResult parsed = PowersConfigLoader.parseWithReport("""
				{
				  "schemaVersion": 4,
				  "policyOverrides": {
				    "worlds": {
				      "acceptance world": {
				        "allowTerrainDamage": false,
				        "requireLocatorConsent": false
				      }
				    },
				    "dimensions": {
				      "minecraft:the_nether": {
				        "allowTerrainDamage": true
				      }
				    }
				  }
				}
				""");

		ResolvedPowerPolicy policy = ResolvedPowerPolicy.resolve(
				parsed.config(), "acceptance world", "minecraft:the_nether");
		assertTrue(policy.allowTerrainDamage());
		assertEquals(ResolvedPowerPolicy.Scope.DIMENSION,
				policy.source(ResolvedPowerPolicy.Field.ALLOW_TERRAIN_DAMAGE).scope());
		assertEquals(ResolvedPowerPolicy.Scope.WORLD,
				policy.source(ResolvedPowerPolicy.Field.REQUIRE_LOCATOR_CONSENT).scope());
	}

	@Test
	void malformedPolicyPatchesFailClosedAndAreReportedWithoutRawValues() {
		PowersConfigLoader.ParseResult parsed = PowersConfigLoader.parseWithReport("""
				{
				  "schemaVersion": 4,
				  "policyOverrides": {
				    "dimensions": {
				      "NOT A DIMENSION": {"allowTerrainDamage": true},
				      "minecraft:overworld": {"allowTerrainDamage": "yes"}
				    }
				  }
				}
				""");

		assertTrue(parsed.config().policyOverrides().dimensions().isEmpty());
		String report = String.join("\n", parsed.report().operatorLines());
		assertTrue(report.contains("policyOverrides.dimensions"));
		assertTrue(!report.contains("NOT A DIMENSION") && !report.contains("yes"));
	}

	private static ConfigValidationReport.Entry entry(ConfigValidationReport report, String path) {
		return report.entries().stream().filter(value -> value.path().equals(path))
				.filter(value -> value.reason().equals("out_of_range") || value.reason().equals("invalid_type"))
				.findFirst().orElseThrow();
	}
}
