package com.powers.testing;

import org.junit.jupiter.api.Test;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestingArenaLayoutTest {
	@Test
	void arenaIsBoundedAndGivesEveryTargetAUniqueReadablePosition() {
		var targets = TestingArenaLayout.targets();
		assertEquals(7, targets.size());
		var positions = new HashSet<String>();
		for (var target : targets) {
			assertTrue(positions.add(target.x() + ":" + target.z()), target.toString());
			assertTrue(Math.abs(target.x()) <= 8 && Math.abs(target.z()) <= 8, target.toString());
			assertTrue(!target.name().isBlank(), target.toString());
		}
	}
}
