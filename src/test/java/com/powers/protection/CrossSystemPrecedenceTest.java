package com.powers.protection;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CrossSystemPrecedenceTest {
	@Test
	void conflictsUseTheDocumentedSafetyFirstOrder() {
		assertEquals(CrossSystemPrecedence.Guard.REALM_CONFINEMENT,
				CrossSystemPrecedence.first(EnumSet.allOf(CrossSystemPrecedence.Guard.class)));
		assertEquals(CrossSystemPrecedence.Guard.SAFE_ZONE,
				CrossSystemPrecedence.first(EnumSet.of(CrossSystemPrecedence.Guard.SAFE_ZONE,
						CrossSystemPrecedence.Guard.AMETHYST,
						CrossSystemPrecedence.Guard.FORCEFIELD)));
		assertEquals(CrossSystemPrecedence.Guard.AMETHYST,
				CrossSystemPrecedence.first(EnumSet.of(CrossSystemPrecedence.Guard.AMETHYST,
						CrossSystemPrecedence.Guard.FORCEFIELD,
						CrossSystemPrecedence.Guard.DIMENSIONAL_ANCHOR)));
		assertEquals(CrossSystemPrecedence.Guard.DIMENSIONAL_ANCHOR,
				CrossSystemPrecedence.first(EnumSet.of(CrossSystemPrecedence.Guard.DIMENSIONAL_ANCHOR,
						CrossSystemPrecedence.Guard.CONSENT_OVERRIDE)));
	}
}
