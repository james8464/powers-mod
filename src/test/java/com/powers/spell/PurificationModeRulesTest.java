package com.powers.spell;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PurificationModeRulesTest {
	@Test
	void catalystFamiliesChooseOneModeAndUnknownItemsFailSafeToCleanse() {
		assertEquals(PurificationModeRules.Mode.CLEANSE,
				PurificationModeRules.fromCatalyst("powers:imported_magic_essence_sacred_dust"));
		assertEquals(PurificationModeRules.Mode.CLEANSE,
				PurificationModeRules.fromCatalyst("powers:imported_artifact_runestone_inert"));
		assertEquals(PurificationModeRules.Mode.LINK_SEVER,
				PurificationModeRules.fromCatalyst("powers:imported_magic_essence_soul_dust"));
		assertEquals(PurificationModeRules.Mode.LINK_SEVER,
				PurificationModeRules.fromCatalyst("powers:imported_artifact_bound_runestone_active_2"));
		assertEquals(PurificationModeRules.Mode.CORRUPTION_RELIEF,
				PurificationModeRules.fromCatalyst("powers:imported_magic_essence_blood_dust"));
		assertEquals(PurificationModeRules.Mode.CORRUPTION_RELIEF,
				PurificationModeRules.fromCatalyst("powers:imported_artifact_runestone_dark_large"));
		assertEquals(PurificationModeRules.Mode.CLEANSE,
				PurificationModeRules.fromCatalyst("minecraft:stick"));
		assertEquals(PurificationModeRules.Mode.CLEANSE,
				PurificationModeRules.fromCatalyst("thirdparty:soul_dust"));
		assertEquals(PurificationModeRules.Mode.CLEANSE,
				PurificationModeRules.fromCatalyst(null));
	}

	@Test
	void cleanseNeverRemovesAmethystOrCorruptionState() {
		assertTrue(PurificationModeRules.removesEffect(
				PurificationModeRules.Mode.CLEANSE, true, false, false));
		assertFalse(PurificationModeRules.removesEffect(
				PurificationModeRules.Mode.CLEANSE, true, true, false));
		assertFalse(PurificationModeRules.removesEffect(
				PurificationModeRules.Mode.CLEANSE, true, false, true));
	}

	@Test
	void corruptionReliefOnlyRemovesOwnedCorruptionEffect() {
		assertTrue(PurificationModeRules.removesEffect(
				PurificationModeRules.Mode.CORRUPTION_RELIEF, true, false, true));
		assertFalse(PurificationModeRules.removesEffect(
				PurificationModeRules.Mode.CORRUPTION_RELIEF, true, false, false));
		assertFalse(PurificationModeRules.removesEffect(
				PurificationModeRules.Mode.LINK_SEVER, true, false, true));
	}
}
