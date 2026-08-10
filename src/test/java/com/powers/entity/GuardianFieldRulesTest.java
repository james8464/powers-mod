package com.powers.entity;

import com.powers.item.artifact.ArtifactAlignment;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuardianFieldRulesTest {
	@Test
	void fieldsPulseOnBoundedTacticalCadences() {
		assertTrue(GuardianFieldRules.pulseAt(120, false));
		assertFalse(GuardianFieldRules.pulseAt(121, false));
		assertTrue(GuardianFieldRules.pulseAt(80, true));
		assertFalse(GuardianFieldRules.pulseAt(120, true));
	}

	@Test
	void alignedFieldsHarmOnlyTheOpposedFaction() {
		assertTrue(GuardianFieldRules.hostile(ArtifactAlignment.DARKNESS, false));
		assertFalse(GuardianFieldRules.hostile(ArtifactAlignment.DARKNESS, true));
		assertTrue(GuardianFieldRules.hostile(ArtifactAlignment.LIGHT, true));
		assertFalse(GuardianFieldRules.hostile(ArtifactAlignment.LIGHT, false));
	}
}
