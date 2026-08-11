package com.powers.item;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FantasyWeaponArchetypeTest {
	@Test
	void namesResolveToSpecificCombatIdentities() {
		assertEquals(FantasyWeaponArchetype.FROST, FantasyWeaponArchetype.from("winterthorn"));
		assertEquals(FantasyWeaponArchetype.SWIFT, FantasyWeaponArchetype.from("azure_dagger"));
		assertEquals(FantasyWeaponArchetype.REAPER, FantasyWeaponArchetype.from("azure_scythe"));
		assertEquals(FantasyWeaponArchetype.CRUSHER, FantasyWeaponArchetype.from("iron_mace"));
		assertEquals(FantasyWeaponArchetype.BERSERKER, FantasyWeaponArchetype.from("crimson_cleaver"));
		assertEquals(FantasyWeaponArchetype.ARCANE, FantasyWeaponArchetype.from("void_oculus"));
	}

	@Test
	void everyArchetypeHasDifferentStatsAndABoundedProc() {
		var profiles = Arrays.stream(FantasyWeaponArchetype.values())
				.map(type -> type.damage() + ":" + type.speed()).distinct().count();
		assertEquals(FantasyWeaponArchetype.values().length, profiles);
		assertTrue(Arrays.stream(FantasyWeaponArchetype.values())
				.allMatch(type -> type.procCooldownTicks() >= 20 && type.procCooldownTicks() <= 120));
	}
}
