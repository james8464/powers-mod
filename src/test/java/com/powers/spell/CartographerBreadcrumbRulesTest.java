package com.powers.spell;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CartographerBreadcrumbRulesTest {
	@Test
	void breadcrumbsAreLocalFiniteAndDirectionPreserving() {
		assertEquals(10, CartographerBreadcrumbRules.pulseCount());
		assertEquals(20, CartographerBreadcrumbRules.pulseIntervalTicks());
		var points = CartographerBreadcrumbRules.localTrail(300, -400);
		assertEquals(4, points.size());
		assertTrue(points.stream().allMatch(point -> Math.hypot(point.x(), point.z()) <= 8.0001));
		assertTrue(points.getLast().x() > 0 && points.getLast().z() < 0);
		assertTrue(CartographerBreadcrumbRules.localTrail(0, 0).isEmpty());
	}
}
