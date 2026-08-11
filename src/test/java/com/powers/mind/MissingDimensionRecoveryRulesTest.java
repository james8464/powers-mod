package com.powers.mind;

import com.powers.power.travel.TravelKind;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MissingDimensionRecoveryRulesTest {
	@Test
	void onlyAdministrativeRecoveryMayReplaceADeletedBodyDimension() {
		assertTrue(MissingDimensionRecoveryRules.useOverworldFallback(
				TravelKind.ADMIN_RECOVERY, false));
		assertFalse(MissingDimensionRecoveryRules.useOverworldFallback(
				TravelKind.PLAYER_RETURN, false));
		assertFalse(MissingDimensionRecoveryRules.useOverworldFallback(
				TravelKind.FATAL_SOUL_RETURN, false));
		assertFalse(MissingDimensionRecoveryRules.useOverworldFallback(
				TravelKind.ADMIN_RECOVERY, true));
	}
}
