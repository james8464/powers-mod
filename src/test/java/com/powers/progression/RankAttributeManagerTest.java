package com.powers.progression;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RankAttributeManagerTest {
	@Test
	void desiredModifiersAreStableOwnedAndBounded() {
		RankProfile profile = new RankProfile(
				Map.of(RankPerkType.MOVEMENT, 0.35, RankPerkType.RESISTANCE, 0.20,
						RankPerkType.WARD_INTEGRITY, 0.50), Map.of(), Map.of(), "focus");
		var modifiers = RankAttributeManager.specifications(profile);

		assertEquals(3, modifiers.size());
		assertEquals(3, modifiers.stream().map(RankAttributeManager.ModifierSpec::id).distinct().count());
		assertTrue(modifiers.stream().allMatch(spec -> spec.id().getNamespace().equals("powers")));
		assertTrue(modifiers.stream().allMatch(spec -> Double.isFinite(spec.amount()) && spec.amount() >= 0));
	}
}
