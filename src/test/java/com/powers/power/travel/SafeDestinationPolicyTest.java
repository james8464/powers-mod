package com.powers.power.travel;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SafeDestinationPolicyTest {
	@Test
	void maximumBuildHeightIsExclusive() {
		assertEquals(DestinationFailure.OUT_OF_BOUNDS,
				SafeDestinationResolver.boundsFailure(0, 320, 0, -64, 320,
						-1000, 1000, -1000, 1000));
	}

	@Test
	void rejectsNanAndInfinity() {
		assertEquals(DestinationFailure.OUT_OF_BOUNDS,
				SafeDestinationResolver.boundsFailure(Double.NaN, 64, 0, -64, 320,
						-1000, 1000, -1000, 1000));
		assertEquals(DestinationFailure.OUT_OF_BOUNDS,
				SafeDestinationResolver.boundsFailure(0, 64, Double.POSITIVE_INFINITY, -64, 320,
						-1000, 1000, -1000, 1000));
	}

	@Test
	void acceptsAFinitePointInsideBorderAndHeight() {
		assertEquals(DestinationFailure.NONE,
				SafeDestinationResolver.boundsFailure(10, 64, -10, -64, 320,
						-1000, 1000, -1000, 1000));
	}

	@Test
	void middleworldAcceptsItsCrystalButNotOrdinaryTeleportPowers() {
		assertEquals(DestinationFailure.REALM_RESTRICTED,
				SafeDestinationResolver.realmFailure(true, TravelKind.POWER));
		assertEquals(DestinationFailure.NONE,
				SafeDestinationResolver.realmFailure(true, TravelKind.CRYSTAL));
	}
}
