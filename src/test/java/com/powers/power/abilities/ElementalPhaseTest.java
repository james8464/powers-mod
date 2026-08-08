package com.powers.power.abilities;

import com.powers.magic.MagicActionCatalogue;
import com.powers.magic.MagicActionId;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/** Protects persisted phase normalization and canonical transaction identity. */
class ElementalPhaseTest {
	@Test
	void phasesMapToExistingCanonicalActionsAndAuthoredColours() {
		List<String> actionIds = List.of("fireball", "frost_nova", "lightning_strike", "ground_slam");
		List<Integer> colors = List.of(0xFF5A24, 0x82E9FF, 0xFFF59D, 0x8C66FF);
		MagicActionCatalogue catalogue = MagicActionCatalogue.defaults();

		for (int index = 0; index < actionIds.size(); index++) {
			ElementalPhase phase = ElementalPhase.fromIndex(index);
			assertEquals(index, phase.index());
			assertEquals(actionIds.get(index), phase.actionId());
			assertEquals(colors.get(index), phase.color());
			assertNotNull(catalogue.definition(new MagicActionId(phase.actionId())));
		}
	}

	@Test
	void phaseNavigationWrapsInBothDirections() {
		assertEquals(1, ElementalPhase.nextIndex(0));
		assertEquals(0, ElementalPhase.nextIndex(3));
		assertEquals(3, ElementalPhase.previousIndex(0));
		assertEquals(2, ElementalPhase.previousIndex(3));
	}

	@Test
	void malformedPersistedIndicesNormalizeWithoutArrayFailures() {
		assertEquals(ElementalPhase.EARTH, ElementalPhase.fromIndex(-1));
		assertEquals(ElementalPhase.FLAME, ElementalPhase.fromIndex(4));
		assertEquals(ElementalPhase.FROST, ElementalPhase.fromIndex(1_000_001));
		assertEquals(ElementalPhase.FLAME, ElementalPhase.fromIndex(Integer.MIN_VALUE));
	}
}
