package com.powers.magic.participant;

import com.powers.knowledge.MagicFailureReason;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ParticipantCapabilityContractTest {
	@Test
	void ordinaryLivingTargetsGetTypedFailureForPlayerOnlyState() {
		var result = ParticipantCapabilityContract.check(MagicParticipants.Kind.NONE,
				ParticipantCapability.PLAYER_POWER_STATE);
		assertFalse(result.supported());
		assertEquals(MagicFailureReason.UNSUPPORTED_TARGET, result.failure());
	}

	@Test
	void actorCapabilitiesAreExplicitRatherThanAssumedByCast() {
		assertTrue(ParticipantCapabilityContract.check(MagicParticipants.Kind.PLAYER,
				ParticipantCapability.PLAYER_POWER_STATE).supported());
		assertFalse(ParticipantCapabilityContract.check(MagicParticipants.Kind.TEST_ACTOR,
				ParticipantCapability.PLAYER_POWER_STATE).supported());
		assertTrue(ParticipantCapabilityContract.check(MagicParticipants.Kind.SHADOW,
				ParticipantCapability.ENERGY_POOL).supported());
	}
}
