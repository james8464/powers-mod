package com.powers.migration;

import com.powers.power.PowerAffinity;
import com.powers.power.PowerRegistry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SaveMigrationRulesTest {
	@BeforeAll
	static void registerPowers() {
		PowerRegistry.initialize();
	}

	@Test
	void anEmptyNeverAssignedSaveRemainsUnassigned() {
		assertEquals(List.of(), SaveMigrationRules.canonicalPowerSlots(List.of(), PowerAffinity.RADIANT));
	}

	@Test
	void partialAndRetiredLoadoutsBecomeStableThreeSlotSaves() {
		List<String> partial = List.of("powers:flight", "powers:ground_slam");
		List<String> canonical = SaveMigrationRules.canonicalPowerSlots(partial, PowerAffinity.DARKNESS);

		assertEquals(List.of("powers:flight", "powers:forcefield", "powers:energy_drain"), canonical);
		assertEquals(canonical, SaveMigrationRules.canonicalPowerSlots(canonical, PowerAffinity.DARKNESS));
	}

	@Test
	void excessAndDuplicateSlotsAreBoundedWithoutDiscardingValidOrder() {
		List<String> canonical = SaveMigrationRules.canonicalPowerSlots(List.of(
				"powers:flight", "powers:flight", "powers:fireball", "powers:forcefield"),
				PowerAffinity.RADIANT);

		assertEquals(List.of("powers:flight", "powers:fireball", "powers:starfall"), canonical);
	}
}
