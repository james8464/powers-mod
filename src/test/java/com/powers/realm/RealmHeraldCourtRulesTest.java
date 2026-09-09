package com.powers.realm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RealmHeraldCourtRulesTest {
	@Test
	void healthSelectsThreeStableCourtPhases() {
		assertEquals(RealmHeraldCourtRules.Phase.VERDICT,
				RealmHeraldCourtRules.phase(1.0F));
		assertEquals(RealmHeraldCourtRules.Phase.VERDICT,
				RealmHeraldCourtRules.phase(0.67F));
		assertEquals(RealmHeraldCourtRules.Phase.ASCENDANT,
				RealmHeraldCourtRules.phase(2.0F / 3.0F));
		assertEquals(RealmHeraldCourtRules.Phase.ASCENDANT,
				RealmHeraldCourtRules.phase(0.34F));
		assertEquals(RealmHeraldCourtRules.Phase.LAST_COVENANT,
				RealmHeraldCourtRules.phase(1.0F / 3.0F));
		assertEquals(RealmHeraldCourtRules.Phase.LAST_COVENANT,
				RealmHeraldCourtRules.phase(Float.NaN));
	}

	@Test
	void pressureScalesWithinHardPlayerAndSummonCaps() {
		var solo = RealmHeraldCourtRules.profile(RealmHeraldCourtRules.Phase.VERDICT, 1);
		var raid = RealmHeraldCourtRules.profile(RealmHeraldCourtRules.Phase.LAST_COVENANT, 99);

		assertEquals(1, solo.participants());
		assertEquals(RealmHeraldCourtRules.MAX_PARTICIPANTS, raid.participants());
		assertTrue(raid.beamDamage() > solo.beamDamage());
		assertTrue(raid.summonCap() > solo.summonCap());
		assertTrue(raid.summonCap() <= RealmHeraldCourtRules.MAX_SUMMONS);
		assertTrue(raid.temporalSummonCeiling() <= RealmHeraldCourtRules.MAX_SUMMONS);
		assertTrue(raid.hazardRadius() <= RealmHeraldCourtRules.MAX_HAZARD_RADIUS);
	}

	@Test
	void everyHazardExpiresAndNoCourtStateCanLockRecoveryExit() {
		for (RealmHeraldCourtRules.Phase phase : RealmHeraldCourtRules.Phase.values()) {
			var profile = RealmHeraldCourtRules.profile(phase, 4);
			assertTrue(profile.hazardDurationTicks() > profile.hazardPulseTicks());
			assertTrue(profile.hazardDurationTicks()
					<= profile.hazardPulseTicks() + RealmHeraldCourtRules.MAX_HAZARD_TAIL_TICKS);
			assertFalse(profile.blocksRecoveryExit());
		}
	}

	@Test
	void restoredValuesAreSanitizedBeforeRuntimeUse() {
		assertEquals(RealmHeraldCourtRules.Phase.VERDICT,
				RealmHeraldCourtRules.restoredPhase(-100));
		assertEquals(RealmHeraldCourtRules.Phase.LAST_COVENANT,
				RealmHeraldCourtRules.restoredPhase(100));
		assertEquals(1, RealmHeraldCourtRules.normalizeParticipants(-1));
		assertEquals(RealmHeraldCourtRules.MAX_PARTICIPANTS,
				RealmHeraldCourtRules.normalizeParticipants(Integer.MAX_VALUE));
	}
}
