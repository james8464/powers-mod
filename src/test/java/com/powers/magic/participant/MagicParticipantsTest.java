package com.powers.magic.participant;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MagicParticipantsTest {
	@Test
	void participantKindsKeepDistinctConsentAndAlignmentPolicies() {
		assertEquals(MagicConsentAuthority.PLAYER_SETTINGS,
				MagicParticipants.policy(MagicParticipants.Kind.PLAYER).consent());
		assertEquals(MagicConsentAuthority.ALWAYS_ALLOW_TESTS,
				MagicParticipants.policy(MagicParticipants.Kind.TEST_ACTOR).consent());
		assertEquals(MagicConsentAuthority.OWNER_DELEGATED,
				MagicParticipants.policy(MagicParticipants.Kind.SHADOW).consent());
		assertEquals(MagicParticipant.Alignment.DARKNESS,
				MagicParticipants.policy(MagicParticipants.Kind.SHADOW).alignment());
	}
}
