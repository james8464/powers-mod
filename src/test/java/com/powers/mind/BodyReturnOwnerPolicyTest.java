package com.powers.mind;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BodyReturnOwnerPolicyTest {
	@Test
	void lifecycleReferenceWinsWhenPlayerListLookupIsNotYetAvailable() {
		assertEquals(BodyReturnOwnerPolicy.Source.DIRECT,
				BodyReturnOwnerPolicy.resolve(true, false));
	}

	@Test
	void delayedCompletionUsesTheCurrentListedPlayer() {
		assertEquals(BodyReturnOwnerPolicy.Source.LOOKUP,
				BodyReturnOwnerPolicy.resolve(false, true));
		assertEquals(BodyReturnOwnerPolicy.Source.MISSING,
				BodyReturnOwnerPolicy.resolve(false, false));
	}
}
