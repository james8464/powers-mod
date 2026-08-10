package com.powers.entity;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.powers.item.artifact.ArtifactAlignment;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GuardianFactionRulesTest {
	private static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-000000000001");
	private static final UUID STRANGER = UUID.fromString("00000000-0000-0000-0000-000000000002");

	@Test
	void guardiansNeverInvertOwnerOrFactionTargeting() {
		assertFalse(GuardianFactionRules.mayTarget(ArtifactAlignment.DARKNESS, OWNER, OWNER, true));
		assertFalse(GuardianFactionRules.mayTarget(ArtifactAlignment.DARKNESS, OWNER, STRANGER, true));
		assertTrue(GuardianFactionRules.mayTarget(ArtifactAlignment.DARKNESS, OWNER, STRANGER, false));
		assertFalse(GuardianFactionRules.mayTarget(ArtifactAlignment.LIGHT, OWNER, STRANGER, false));
		assertTrue(GuardianFactionRules.mayTarget(ArtifactAlignment.LIGHT, OWNER, STRANGER, true));
	}

	@Test
	void ownedSummonsExpireWhileNaturalRealmCreaturesPersist() {
		assertTrue(GuardianFactionRules.shouldExpire(0, true));
		assertFalse(GuardianFactionRules.shouldExpire(-1, true));
		assertTrue(GuardianFactionRules.shouldExpire(50, false));
	}
}
