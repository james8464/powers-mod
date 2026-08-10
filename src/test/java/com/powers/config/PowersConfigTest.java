package com.powers.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.powers.protection.PowerProtection;
import com.powers.protection.ProtectionDecision;

class PowersConfigTest {
	@Test
	void defaultsEnableCombatScarsButProtectValuableBlocksAndPlayers() {
		PowersConfig config = PowersConfig.defaults();

		assertEquals(PowersConfig.CURRENT_SCHEMA_VERSION, config.schemaVersion());
		assertTrue(config.allowTerrainDamage());
		assertFalse(config.allowBlockEntityDamage());
		assertFalse(config.allowSelfReroll());
		assertFalse(config.hostileForcedMovement());
		assertTrue(config.requireTeleportConsent());
		assertTrue(config.requireLocatorConsent());
		assertTrue(config.requireCompanionConsent());
		assertTrue(config.requireDreamwalkConsent());
		assertTrue(config.requirePossessionConsent());
		assertTrue(config.projectionBodiesVulnerable());
		assertTrue(config.persistCooldowns());
		assertTrue(config.celestialRuinTerrainDamage());
		assertTrue(config.celestialRuinBlockEntityDamage());
		assertTrue(config.maxParticlesPerTick() > 0);
		assertTrue(config.spaceTimeRadius() > 0);
		assertTrue(config.chronoStopRadius() > 0);
		assertTrue(config.rankRespecExperienceLevels() > 0);
		assertTrue(config.livingForces().spreadingEnabled());
		assertEquals(48, config.livingForces().clashRadius());
		assertEquals(2, config.livingForces().witherAmplifier());
		assertFalse(config.dialogueProvider().enabled());
		assertEquals(4, config.dialogueProvider().maxGlobalRequests());
	}

	@Test
	void sanitizationClampsUnsafeNumericValues() {
		PowersConfig invalid = new PowersConfig(-10, false, false, false, false, true, true, true,
				true, true, true, true, true, true, -5, 0, 0, 500, 500, 5000, 99, java.util.List.of(),
				new PowersConfig.LivingForces(true, -1, -2, -3, -4, 1000, 1),
				new PowersConfig.DialogueProvider(true, " endpoint ", " model ", "bad variable!",
						99_999, 999, 1));

		PowersConfig sanitized = invalid.sanitized();
		assertEquals(PowersConfig.CURRENT_SCHEMA_VERSION, sanitized.schemaVersion());
		assertEquals(1, sanitized.wardRadius());
		assertEquals(32, sanitized.maxParticlesPerTick());
		assertEquals(1, sanitized.teleportMaxChunkDistance());
		assertEquals(128, sanitized.spaceTimeRadius());
		assertEquals(256, sanitized.chronoStopRadius());
		assertEquals(1000, sanitized.rankRespecExperienceLevels());
		assertEquals(4, sanitized.adminPermissionLevel());
		assertEquals(1, sanitized.livingForces().spreadAttempts());
		assertEquals(1, sanitized.livingForces().auraRadius());
		assertEquals(0, sanitized.livingForces().witherAmplifier());
		assertEquals(1, sanitized.livingForces().energyRefillPerSecond());
		assertEquals(96, sanitized.livingForces().clashRadius());
		assertEquals(256, sanitized.livingForces().clashChecksPerTick());
		assertEquals(2_500, sanitized.dialogueProvider().timeoutMillis());
		assertEquals(4, sanitized.dialogueProvider().maxGlobalRequests());
		assertEquals(10, sanitized.dialogueProvider().ownerCooldownSeconds());
	}

	@Test
	void ordinaryTerrainDefaultsAllowCombatScars() {
		assertEquals(ProtectionDecision.ALLOW,
				PowerProtection.blockDecision(PowersConfig.defaults(), false, false));
		assertEquals(ProtectionDecision.DENY_TERRAIN, PowerProtection.blockDecision(
				PowersConfigLoader.parse("{\"schemaVersion\":2,\"allowTerrainDamage\":false}"),
				false, false));
	}

	@Test
	void missingJsonFieldsKeepSafeDefaults() {
		PowersConfig config = PowersConfigLoader.parse("{}");
		assertTrue(config.projectionBodiesVulnerable());
		assertTrue(config.requireTeleportConsent());
		assertTrue(config.allowTerrainDamage());
		assertEquals(512, config.maxParticlesPerTick());
		assertTrue(config.livingForces().spreadingEnabled());
		assertEquals(4096, config.livingForces().clashChecksPerTick());
		assertFalse(PowersConfigLoader.parse("{\"celestialRuinTerrainDamage\":false}")
				.celestialRuinTerrainDamage());
	}

	@Test
	void legacyGeneratedTerrainDefaultMigratesOnceButVersionTwoOptOutRemains() {
		assertTrue(PowersConfigLoader.parse("{\"allowTerrainDamage\":false}")
				.allowTerrainDamage());
		assertFalse(PowersConfigLoader.parse(
				"{\"schemaVersion\":2,\"allowTerrainDamage\":false}")
				.allowTerrainDamage());
	}

	@Test
	void malformedOrExcessiveSafeZonesBecomeFiniteAndBounded() {
		java.util.List<PowersConfig.SafeZone> zones = new java.util.ArrayList<>();
		zones.add(new PowersConfig.SafeZone(" invalid realm ", Double.NaN,
				Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NaN));
		for (int index = 0; index < 300; index++) {
			zones.add(new PowersConfig.SafeZone("minecraft:overworld", index, 64, index, 10));
		}
		PowersConfig defaults = PowersConfig.defaults();
		PowersConfig sanitized = new PowersConfig(defaults.schemaVersion(),
				defaults.allowTerrainDamage(), defaults.allowBlockEntityDamage(),
				defaults.allowSelfReroll(), defaults.hostileForcedMovement(),
				defaults.requireTeleportConsent(), defaults.requireLocatorConsent(),
				defaults.requireCompanionConsent(), defaults.requireDreamwalkConsent(),
				defaults.requirePossessionConsent(), defaults.projectionBodiesVulnerable(),
				defaults.persistCooldowns(), defaults.celestialRuinTerrainDamage(),
				defaults.celestialRuinBlockEntityDamage(), defaults.wardRadius(),
				defaults.maxParticlesPerTick(), defaults.teleportMaxChunkDistance(),
				defaults.spaceTimeRadius(), defaults.chronoStopRadius(),
				defaults.rankRespecExperienceLevels(), defaults.adminPermissionLevel(), zones,
				defaults.livingForces(), defaults.dialogueProvider()).sanitized();

		assertEquals(256, sanitized.safeZones().size());
		PowersConfig.SafeZone first = sanitized.safeZones().getFirst();
		assertEquals("minecraft:overworld", first.dimension());
		assertEquals(0.0, first.x());
		assertEquals(0.0, first.y());
		assertEquals(0.0, first.z());
		assertEquals(1.0, first.radius());
	}
}
