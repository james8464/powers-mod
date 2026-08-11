package com.powers.power.state;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalTimeStopRulesTest {
	private static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-000000000001");
	private static final UUID OTHER = UUID.fromString("00000000-0000-0000-0000-000000000002");

	@Test
	void doesNotStealAnAdministrativeOrAnotherPlayersFreeze() {
		assertFalse(GlobalTimeStopRules.mayStart(false, true));
		assertFalse(GlobalTimeStopRules.mayStart(true, true));
		assertTrue(GlobalTimeStopRules.mayStart(false, false));
	}

	@Test
	void onlyTheOwnerMayActWhileGlobalTimeIsStopped() {
		assertTrue(GlobalTimeStopRules.mayAct(OWNER, OWNER));
		assertFalse(GlobalTimeStopRules.mayAct(OWNER, OTHER));
		assertTrue(GlobalTimeStopRules.mayAct(null, OTHER));
	}

	@Test
	void lifecycleFailureAlwaysReleasesOwnedTime() {
		assertTrue(GlobalTimeStopRules.shouldRelease(false, true, true, false, true));
		assertTrue(GlobalTimeStopRules.shouldRelease(true, false, true, false, true));
		assertTrue(GlobalTimeStopRules.shouldRelease(true, true, false, false, true));
		assertTrue(GlobalTimeStopRules.shouldRelease(true, true, true, true, true));
		assertTrue(GlobalTimeStopRules.shouldRelease(true, true, true, false, false));
		assertFalse(GlobalTimeStopRules.shouldRelease(true, true, true, false, true));
	}

	@Test
	void externalClockMutationEndsPowerOwnershipWithoutUndoingAdministratorState() {
		assertTrue(GlobalTimeStopRules.shouldRelease(true, true, true,
				false, true, true));
		assertFalse(GlobalTimeStopRules.shouldUnfreezeOnRelease(true, true));
		assertTrue(GlobalTimeStopRules.shouldUnfreezeOnRelease(true, false));
		assertFalse(GlobalTimeStopRules.shouldUnfreezeOnRelease(false, false));
	}

	@Test void ownerHudUsesTheAuthoritativeDeadline() {
		assertEquals(1200L, GlobalTimeStopRules.remainingTicks(500, 1700));
		assertEquals(0L, GlobalTimeStopRules.remainingTicks(1701, 1700));
	}
}
