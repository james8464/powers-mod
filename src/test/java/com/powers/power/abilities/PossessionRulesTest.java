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
}
