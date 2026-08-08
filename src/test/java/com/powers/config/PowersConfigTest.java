package com.powers.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.powers.protection.PowerProtection;
import com.powers.protection.ProtectionDecision;

class PowersConfigTest {
	@Test
	void defaultsProtectPersistentMultiplayerWorlds() {
		PowersConfig config = PowersConfig.defaults();

		assertFalse(config.allowTerrainDamage());
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
		assertTrue(config.maxParticlesPerTick() > 0);
		assertTrue(config.spaceTimeRadius() > 0);
		assertTrue(config.chronoStopRadius() > 0);
		assertTrue(config.rankRespecExperienceLevels() > 0);
		assertTrue(config.livingForces().spreadingEnabled());
		assertEquals(48, config.livingForces().clashRadius());
		assertEquals(2, config.livingForces().witherAmplifier());
	}

	@Test
	void sanitizationClampsUnsafeNumericValues() {
		PowersConfig invalid = new PowersConfig(false, false, false, false, true, true, true,
				true, true, true, true, -5, 0, 0, 500, 500, 5000, 99, java.util.List.of(),
				new PowersConfig.LivingForces(true, -1, -2, -3, -4, 1000, 1));

		PowersConfig sanitized = invalid.sanitized();
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
	}

	@Test
	void protectedTerrainDefaultsRejectEveryBlockChange() {
		assertEquals(ProtectionDecision.DENY_TERRAIN,
				PowerProtection.blockDecision(PowersConfig.defaults(), false, false));
	}

	@Test
	void missingJsonFieldsKeepSafeDefaults() {
		PowersConfig config = PowersConfigLoader.parse("{}");
		assertTrue(config.projectionBodiesVulnerable());
		assertTrue(config.requireTeleportConsent());
		assertFalse(config.allowTerrainDamage());
		assertEquals(512, config.maxParticlesPerTick());
		assertTrue(config.livingForces().spreadingEnabled());
		assertEquals(4096, config.livingForces().clashChecksPerTick());
	}
}
