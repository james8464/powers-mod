package com.powers.power.abilities;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

/** Regression coverage for block impacts, which do not have a living direct target. */
class FireballImpactResolverTest {
	@Test
	void blockImpactCannotDereferenceAMissingTargetWhenCheckingForcefields() {
		assertFalse(FireballImpactResolver.hasForcefield(null, null));
	}
}
