package com.powers.config;

import com.powers.protection.PowerProtection;
import com.powers.protection.ProtectionDecision;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResolvedPowerPolicyTest {
	@Test
	void dimensionOverridesWorldWhichOverridesGlobalPerField() {
		PowerPolicyPatch world = patch(false, true, false);
		PowerPolicyPatch dimension = patch(true, null, null);
		PowersConfig config = PowersConfig.defaults().withPolicyOverrides(new PowerPolicyOverrides(
				Map.of("acceptance-world", world),
				Map.of("minecraft:the_nether", dimension)));

		ResolvedPowerPolicy resolved = ResolvedPowerPolicy.resolve(
				config, "acceptance-world", "minecraft:the_nether");

		assertTrue(resolved.allowTerrainDamage());
		assertTrue(resolved.allowBlockEntityDamage());
		assertFalse(resolved.requireLocatorConsent());
		assertEquals(ResolvedPowerPolicy.Scope.DIMENSION,
				resolved.source(ResolvedPowerPolicy.Field.ALLOW_TERRAIN_DAMAGE).scope());
		assertEquals(ResolvedPowerPolicy.Scope.WORLD,
				resolved.source(ResolvedPowerPolicy.Field.ALLOW_BLOCK_ENTITY_DAMAGE).scope());
		assertEquals(ResolvedPowerPolicy.Scope.WORLD,
				resolved.source(ResolvedPowerPolicy.Field.REQUIRE_LOCATOR_CONSENT).scope());
		assertEquals(ResolvedPowerPolicy.Scope.GLOBAL,
				resolved.source(ResolvedPowerPolicy.Field.REQUIRE_POSSESSION_CONSENT).scope());
	}

	@Test
	void safeZoneDenialPrecedesEveryResolvedTerrainSetting() {
		ResolvedPowerPolicy policy = ResolvedPowerPolicy.resolve(
				PowersConfig.defaults(), "world", "minecraft:overworld");

		assertEquals(ProtectionDecision.DENY_SAFE_ZONE,
				PowerProtection.blockDecision(policy, true, false));
	}

	@Test
	void externalProtectionDenialCannotBeOverriddenByScopedAllow() {
		ResolvedPowerPolicy policy = ResolvedPowerPolicy.resolve(
				PowersConfig.defaults(), "world", "minecraft:overworld");

		assertEquals(ProtectionDecision.DENY_ADAPTER,
				PowerProtection.blockDecision(policy, false, false, false));
		assertEquals(ProtectionDecision.DENY_SAFE_ZONE,
				PowerProtection.blockDecision(policy, true, false, true));
	}

	@Test
	void diagnosticsNameEveryEffectiveOriginWithoutCoordinates() {
		PowersConfig config = PowersConfig.defaults().withPolicyOverrides(new PowerPolicyOverrides(
				Map.of("world one", patch(false, null, null)),
				Map.of("powers:dark_realm", patch(null, null, false))));

		String line = ResolvedPowerPolicy.resolve(config, "world one", "powers:dark_realm")
				.diagnosticLine("world one", "powers:dark_realm");

		assertTrue(line.contains("terrainDamage=false@world:world one"));
		assertTrue(line.contains("locatorConsent=false@dimension:powers:dark_realm"));
		assertTrue(line.contains("possessionConsent=true@global"));
		assertFalse(line.contains("; x=") || line.contains("; y=") || line.contains("; z="));
	}

	@Test
	void runtimeResolverCachesOneSnapshotPerRevisionAndScope() {
		PowersConfig firstConfig = PowersConfig.defaults();
		ResolvedPowerPolicy first = PowerPolicyResolver.resolve(
				firstConfig, 11L, "world", "minecraft:overworld");

		assertSame(first, PowerPolicyResolver.resolve(
				firstConfig, 11L, "world", "minecraft:overworld"));

		PowersConfig changed = firstConfig.withPolicyOverrides(new PowerPolicyOverrides(
				Map.of(), Map.of("minecraft:overworld", patch(false, null, null))));
		ResolvedPowerPolicy second = PowerPolicyResolver.resolve(
				changed, 12L, "world", "minecraft:overworld");
		assertNotSame(first, second);
		assertFalse(second.allowTerrainDamage());
	}

	@Test
	void everyScopedFieldIsResolvedFromOneImmutableDimensionPatch() {
		PowerPolicyPatch dimension = new PowerPolicyPatch(false, true, true, false,
				false, false, false, false, false, false, true);
		PowersConfig config = PowersConfig.defaults().withPolicyOverrides(
				new PowerPolicyOverrides(Map.of(), Map.of("powers:light_realm", dimension)));

		ResolvedPowerPolicy resolved = ResolvedPowerPolicy.resolve(
				config, null, "powers:light_realm");

		assertFalse(resolved.allowTerrainDamage());
		assertTrue(resolved.allowBlockEntityDamage());
		assertTrue(resolved.hostileForcedMovement());
		assertFalse(resolved.requireTeleportConsent());
		assertFalse(resolved.requireLocatorConsent());
		assertFalse(resolved.requireCompanionConsent());
		assertFalse(resolved.requireDreamwalkConsent());
		assertFalse(resolved.requirePossessionConsent());
		assertFalse(resolved.projectionBodiesVulnerable());
		assertFalse(resolved.celestialRuinTerrainDamage());
		assertTrue(resolved.celestialRuinBlockEntityDamage());
		for (ResolvedPowerPolicy.Field field : ResolvedPowerPolicy.Field.values()) {
			assertEquals(ResolvedPowerPolicy.Scope.DIMENSION, resolved.source(field).scope());
		}
	}

	@Test
	void overrideScopesRejectOversizedKeysAndRemainHardBounded() {
		Map<String, PowerPolicyPatch> dimensions = new java.util.LinkedHashMap<>();
		dimensions.put("test:" + "x".repeat(129), patch(false, null, null));
		for (int index = 0; index < PowerPolicyOverrides.MAX_PER_SCOPE + 20; index++) {
			dimensions.put("test:dimension_" + index, patch(false, null, null));
		}

		PowerPolicyOverrides overrides = new PowerPolicyOverrides(Map.of(), dimensions);

		assertEquals(PowerPolicyOverrides.MAX_PER_SCOPE, overrides.dimensions().size());
		assertTrue(overrides.dimensions().keySet().stream().allMatch(key -> key.length() <= 128));
	}

	@Test
	void unicodeAndWhitespaceWorldNamesRemainExact() {
		String worldName = "  Éclipse 世界  ";
		PowersConfig config = PowersConfig.defaults().withPolicyOverrides(
				new PowerPolicyOverrides(Map.of(worldName, patch(false, null, null)), Map.of()));

		assertFalse(ResolvedPowerPolicy.resolve(config, worldName, "minecraft:overworld")
				.allowTerrainDamage());
		assertTrue(ResolvedPowerPolicy.resolve(config, worldName.strip(), "minecraft:overworld")
				.allowTerrainDamage());
	}

	private static PowerPolicyPatch patch(Boolean terrain, Boolean blockEntities,
			Boolean locatorConsent) {
		return new PowerPolicyPatch(terrain, blockEntities, null, null, locatorConsent,
				null, null, null, null, null, null);
	}
}
