package com.powers.power.damage;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Guards the authoritative damage and Soul Link production entrypoints. */
class LayeredDamageProductionBoundaryTest {
	private static final Path ROOT = Path.of(System.getProperty("user.dir"));

	@Test
	void livingEntityReturnBoundaryOwnsActualHealthAccounting() throws IOException {
		String mixins = Files.readString(ROOT.resolve("src/main/resources/powers.mixins.json"));
		String combatEvents = Files.readString(
				ROOT.resolve("src/main/java/com/powers/PowerCombatEvents.java"));
		String boundary = Files.readString(ROOT.resolve(
				"src/main/java/com/powers/mixin/LivingEntityDamageMixin.java"));

		assertTrue(mixins.contains("\"LivingEntityDamageMixin\""));
		assertTrue(boundary.contains("method = \"hurtServer\""));
		assertTrue(boundary.contains("at = @At(\"HEAD\")"));
		assertTrue(boundary.contains("at = @At(\"RETURN\")"));
		assertTrue(boundary.contains("recordHealthTransition"));
		assertFalse(combatEvents.contains("recordHealth(damageTaken)"));
	}

	@Test
	void serverLifecycleReachesSoulLinkThroughTheCrystalRegistry() throws IOException {
		String lifecycle = Files.readString(
				ROOT.resolve("src/main/java/com/powers/PowersServerLifecycle.java"));
		String registry = Files.readString(ROOT.resolve(
				"src/main/java/com/powers/power/crystals/CrystalPowerRegistry.java"));

		assertTrue(lifecycle.contains("CrystalPowerRegistry.tick(server)"));
		assertTrue(registry.contains("SoulLinkAbility.tickAll(server)"));
	}
}
