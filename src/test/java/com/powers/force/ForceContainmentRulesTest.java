package com.powers.force;

import org.junit.jupiter.api.Test;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForceContainmentRulesTest {
	@Test
	void ceremonyUsesFourCardinalCrystalsAndABoundedSphere() {
		assertEquals(new HashSet<>(ForceContainmentRules.cardinalCrystals()).size(), 4);
		assertTrue(ForceContainmentRules.cardinalCrystals().contains(
				new ForceContainmentRules.Offset(2, 0, 0)));
		assertTrue(ForceContainmentRules.cardinalCrystals().contains(
				new ForceContainmentRules.Offset(0, 0, -2)));
		assertTrue(ForceContainmentRules.sphere().size() < 1_000);
		assertEquals(new ForceContainmentRules.Offset(0, 0, 0),
				ForceContainmentRules.sphere().getFirst());
		assertTrue(ForceContainmentRules.sphere().stream().allMatch(offset ->
				offset.x() * offset.x() + offset.y() * offset.y() + offset.z() * offset.z()
						<= ForceContainmentRules.RADIUS * ForceContainmentRules.RADIUS));
	}
}
