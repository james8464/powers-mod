package com.powers.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerLikeTargetTest {
	private record TestingTarget(String testingUsername) implements PlayerLikeTarget {
	}

	@Test
	void arbitraryMarkerTargetIsCompatibleButCannotBypassConsent() {
		TestingTarget target = new TestingTarget("Test_Alice");

		assertTrue(PlayerLikeTarget.isCompatible(target));
		assertFalse(PlayerLikeTarget.alwaysConsents(target));
	}

	@Test
	void arbitraryObjectIsNeitherCompatibleNorConsenting() {
		Object ordinary = new Object();

		assertFalse(PlayerLikeTarget.isCompatible(ordinary));
		assertFalse(PlayerLikeTarget.alwaysConsents(ordinary));
	}
}
