package com.powers.power.abilities;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PossessionRulesTest {
	@Test
	void livingPlayersAndMobsAreSuitableButOtherLivingEntitiesAreNot() {
		assertTrue(PossessionRules.isSuitable(
				PossessionRules.TargetKind.PLAYER, false, true, false, false));
		assertTrue(PossessionRules.isSuitable(
				PossessionRules.TargetKind.MOB, false, true, false, false));
		assertFalse(PossessionRules.isSuitable(
				PossessionRules.TargetKind.OTHER, false, true, false, false));
	}

	@Test
	void selfDeadRemovedAndBodyProxyTargetsAreRefused() {
		assertFalse(PossessionRules.isSuitable(
				PossessionRules.TargetKind.PLAYER, true, true, false, false));
		assertFalse(PossessionRules.isSuitable(
				PossessionRules.TargetKind.MOB, false, false, false, false));
		assertFalse(PossessionRules.isSuitable(
				PossessionRules.TargetKind.MOB, false, true, true, false));
		assertFalse(PossessionRules.isSuitable(
				PossessionRules.TargetKind.MOB, false, true, false, true));
	}

	@Test
	void consentRemainsMandatoryForPlayersButNotMobs() {
		assertTrue(PossessionRules.requiresPlayerConsent(PossessionRules.TargetKind.PLAYER));
		assertFalse(PossessionRules.requiresPlayerConsent(PossessionRules.TargetKind.MOB));
	}

	@Test
	void aCameraSessionCannotSurviveEitherParticipantChangingDimension() {
		assertTrue(PossessionRules.sessionLocationValid(true, true));
		assertFalse(PossessionRules.sessionLocationValid(false, true));
		assertFalse(PossessionRules.sessionLocationValid(true, false));
	}

	@Test
	void possessionHasAThirtySecondCeilingAndCannotOverpowerAHigherRankPlayer() {
		assertTrue(PossessionRules.durationTicks(200) == 200);
		assertTrue(PossessionRules.durationTicks(5_000) == 600);
		assertTrue(PossessionRules.rankAllows(6, 6));
		assertFalse(PossessionRules.rankAllows(5, 6));
	}

	@Test
	void dreamwalkingUsesTheSharedControlChannelWithoutInnateRankScaling() {
		assertTrue(PossessionRules.allowsCrossDimension(PossessionRules.SessionKind.DREAMWALK));
		assertTrue(PossessionRules.usesDreamwalkProtection(PossessionRules.SessionKind.DREAMWALK));
		assertTrue(PossessionRules.requiresRankCheck(PossessionRules.SessionKind.DREAMWALK));
		assertFalse(PossessionRules.allowsCrossDimension(PossessionRules.SessionKind.POSSESSION));
		assertFalse(PossessionRules.usesDreamwalkProtection(PossessionRules.SessionKind.POSSESSION));
		assertTrue(PossessionRules.requiresRankCheck(PossessionRules.SessionKind.POSSESSION));
	}
}
