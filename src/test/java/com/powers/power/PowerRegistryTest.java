package com.powers.power;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PowerRegistryTest {
	@BeforeAll
	static void bootstrapMinecraftRegistries() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	@Test
	void assignableRosterPromotesSizeMorphingAndRemovesSlowWorldRuntime() {
		PowerRegistry.initialize();
		Set<String> ids = PowerRegistry.getAssignable().stream()
				.map(power -> power.id().getPath())
				.collect(Collectors.toSet());

		assertTrue(ids.contains("size_shift"));
		assertFalse(ids.contains("slow_world"));
		assertNull(PowerRegistry.get("slow_world"));
	}

	@Test
	void retiredPowersAreAbsentFromTheInnateRoster() {
		PowerRegistry.initialize();
		Set<String> ids = PowerRegistry.getAssignable().stream()
				.map(power -> power.id().getPath())
				.collect(Collectors.toSet());

		assertEquals(23, ids.size());
		assertFalse(ids.contains("cozy_campfire"));
		assertFalse(ids.contains("frost_nova"));
		assertFalse(ids.contains("elemental_blast"));
		assertFalse(ids.contains("ground_slam"));
		assertFalse(ids.contains("shadow_step"));
	}

	@Test
	void retiredSelectionsMigrateToStableSafeDefaults() {
		PowerRegistry.initialize();
		List<String> retired = List.of(
				"powers:cozy_campfire", "powers:ground_slam", "powers:shadow_step");

		assertEquals(List.of("powers:flight", "powers:forcefield", "powers:starfall"),
				PowerRegistry.reconcile(retired, PowerAffinity.RADIANT));
		assertEquals(List.of("powers:flight", "powers:forcefield", "powers:energy_drain"),
				PowerRegistry.reconcile(retired, PowerAffinity.DARKNESS));
	}

	@Test
	void affinityRostersExposeOnlyCompatibleInnatePowers() {
		PowerRegistry.initialize();
		List<Power> radiant = PowerRegistry.getAssignable(PowerAffinity.RADIANT);
		List<Power> darkness = PowerRegistry.getAssignable(PowerAffinity.DARKNESS);

		assertTrue(radiant.stream().anyMatch(power -> power.affinity() == PowerAffinity.RADIANT));
		assertTrue(darkness.stream().anyMatch(power -> power.affinity() == PowerAffinity.DARKNESS));
		assertFalse(radiant.stream().anyMatch(power -> power.affinity() == PowerAffinity.DARKNESS));
		assertFalse(darkness.stream().anyMatch(power -> power.affinity() == PowerAffinity.RADIANT));
	}

	@Test
	void randomLoadoutsGuaranteeAnAlignmentExclusivePower() {
		PowerRegistry.initialize();
		for (int seed = 0; seed < 100; seed++) {
			for (PowerAffinity affinity : List.of(PowerAffinity.RADIANT, PowerAffinity.DARKNESS)) {
				List<Power> powers = PowerRegistry.randomDistinct(3, new Random(seed), affinity);
				assertEquals(3, powers.stream().map(Power::id).distinct().count());
				assertTrue(powers.stream().allMatch(power -> power.affinity().permits(affinity)));
				assertTrue(powers.stream().anyMatch(power -> power.affinity() == affinity));
			}
		}
	}

	@Test
	void allegianceMigrationPreservesCompatiblePowersAndReplacesForbiddenOnes() {
		PowerRegistry.initialize();
		List<String> existing = List.of("powers:flight", "powers:starfall", "powers:fireball");
		List<String> migrated = PowerRegistry.reconcile(existing, PowerAffinity.DARKNESS);

		assertTrue(migrated.contains("powers:flight"));
		assertTrue(migrated.contains("powers:fireball"));
		assertFalse(migrated.contains("powers:starfall"));
		assertEquals(3, migrated.stream().distinct().count());
		assertTrue(migrated.stream().map(PowerRegistry::get)
				.anyMatch(power -> power.affinity() == PowerAffinity.DARKNESS));
	}
}
