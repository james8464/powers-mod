package com.powers.power.crystals;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrystalDiscoveryRulesTest {
	private static final List<String> MODES = List.of("inferno", "life_bloom", "middleworld");

	@Test
	void newPlayersBeginWithSelectableButUnknownModes() {
		assertEquals(List.of(), CrystalDiscoveryRules.normalize(MODES, List.of(), OptionalInt.empty()));
		assertFalse(CrystalDiscoveryRules.known(List.of(), "inferno"));
	}

	@Test
	void firstUseDiscoversOneCanonicalModeWithoutDuplicates() {
		assertEquals(List.of("life_bloom"),
				CrystalDiscoveryRules.discover(MODES, List.of(), "life_bloom"));
		assertEquals(List.of("life_bloom"),
				CrystalDiscoveryRules.discover(MODES, List.of("life_bloom"), "life_bloom"));
		assertEquals(List.of(), CrystalDiscoveryRules.discover(MODES, List.of(), "retired_mode"));
		assertTrue(CrystalDiscoveryRules.known(List.of("life_bloom"), "life_bloom"));
	}

	@Test
	void legacySelectionSeedsOnlyItsResolvedModeAndMalformedDataIsDropped() {
		assertEquals(List.of("middleworld"),
				CrystalDiscoveryRules.normalize(MODES,
						List.of("retired", "middleworld", "middleworld"), OptionalInt.empty()));
		assertEquals(List.of("life_bloom"),
				CrystalDiscoveryRules.normalize(MODES, List.of(), OptionalInt.of(1)));
		assertEquals(List.of("middleworld"),
				CrystalDiscoveryRules.normalize(MODES, List.of(), OptionalInt.of(5)));
	}
}
