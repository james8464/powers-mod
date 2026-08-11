package com.powers.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EchoCloneRulesTest {
	@Test
	void clonesFollowAtSixBlocksAndTeleportOnlyPastTwentyFour() {
		assertFalse(EchoCloneRules.shouldFollow(36.0));
		assertTrue(EchoCloneRules.shouldFollow(36.01));
		assertFalse(EchoCloneRules.shouldTeleport(576.0));
		assertTrue(EchoCloneRules.shouldTeleport(576.01));
	}

	@Test
	void echoesRespectTheirOwnersFactionAndNeverAttackOwnedGuardians() {
		assertFalse(EchoCloneRules.mayTarget(true, true, false, false));
		assertTrue(EchoCloneRules.mayTarget(true, false, true, false));
		assertTrue(EchoCloneRules.mayTarget(false, true, false, false));
		assertFalse(EchoCloneRules.mayTarget(false, false, true, false));
		assertFalse(EchoCloneRules.mayTarget(false, true, false, true));
	}
}
