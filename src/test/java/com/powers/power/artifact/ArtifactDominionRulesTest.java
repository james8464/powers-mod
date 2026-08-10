package com.powers.power.artifact;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.powers.item.artifact.ArtifactAlignment;
import org.junit.jupiter.api.Test;

class ArtifactDominionRulesTest {
	@Test
	void decreesScaleAgainstBossesButRespectHardPlayerAndMobCaps() {
		assertEquals(42.0F, ArtifactDominionRules.decreeDamage(
				ArtifactAlignment.DARKNESS, 100.0F, true, 10));
		assertEquals(400.0F, ArtifactDominionRules.decreeDamage(
				ArtifactAlignment.DARKNESS, 10000.0F, true, 10));
		assertEquals(2000.0F, ArtifactDominionRules.decreeDamage(
				ArtifactAlignment.DARKNESS, 10000.0F, false, 10));
		assertEquals(38.0F, ArtifactDominionRules.decreeDamage(
				ArtifactAlignment.LIGHT, 100.0F, true, 10));
	}

	@Test
	void globalFieldAndGuardianCapsRemainFiniteAtCooldownFreeRank() {
		assertTrue(ArtifactDominionRules.mayStartField(3, false));
		assertFalse(ArtifactDominionRules.mayStartField(4, false));
		assertTrue(ArtifactDominionRules.mayStartField(4, true));
		assertEquals(4, ArtifactDominionRules.guardiansToSpawn(12, 0, false));
		assertEquals(2, ArtifactDominionRules.guardiansToSpawn(12, 0, true));
		assertEquals(0, ArtifactDominionRules.guardiansToSpawn(12, 4, false));
	}

	@Test
	void opposedDeathWardsHaveDifferentRestorationProfiles() {
		assertEquals(35.0F, ArtifactDominionRules.restoredHealth(
				ArtifactAlignment.DARKNESS, 100.0F));
		assertEquals(45.0F, ArtifactDominionRules.restoredHealth(
				ArtifactAlignment.LIGHT, 100.0F));
	}
}
