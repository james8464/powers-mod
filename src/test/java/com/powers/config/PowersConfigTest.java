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
		assertTrue(config.projectionBodiesVulnerable());
		assertTrue(config.persistCooldowns());
		assertTrue(config.maxParticlesPerTick() > 0);
	}

	@Test
	void sanitizationClampsUnsafeNumericValues() {
		PowersConfig invalid = new PowersConfig(false, false, false, false, true, true, true,
				true, true, -5, 0, 0, 99, java.util.List.of());

		PowersConfig sanitized = invalid.sanitized();
		assertEquals(1, sanitized.wardRadius());
		assertEquals(32, sanitized.maxParticlesPerTick());
		assertEquals(1, sanitized.teleportMaxChunkDistance());
		assertEquals(4, sanitized.adminPermissionLevel());
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
	}
}
