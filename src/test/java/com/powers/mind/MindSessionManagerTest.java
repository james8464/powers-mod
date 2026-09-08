package com.powers.mind;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MindSessionManagerTest {
	private static final UUID FIRST_SESSION = UUID.fromString("00000000-0000-0000-0000-000000000001");
	private static final UUID SECOND_SESSION = UUID.fromString("00000000-0000-0000-0000-000000000002");
	private static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-000000000010");
	private static final UUID HOST = UUID.fromString("00000000-0000-0000-0000-000000000020");
	private static final UUID OTHER = UUID.fromString("00000000-0000-0000-0000-000000000030");

	@AfterEach
	void clearSessions() {
		MindSessionManager.clear();
	}

	@Test
	void overlappingSessionNamesTheBusyParticipantAndExistingKindWithoutPartialOwnership() {
		MindSessionManager.Admission first = MindSessionManager.acquire(FIRST_SESSION,
				MindSessionManager.Kind.POSSESSION, OWNER, List.of(
						new MindSessionManager.Participant(OWNER, MindSessionManager.Role.OWNER),
						new MindSessionManager.Participant(HOST, MindSessionManager.Role.HOST)));
		MindSessionManager.Admission nested = MindSessionManager.acquire(SECOND_SESSION,
				MindSessionManager.Kind.SOUL_LINK, OTHER, List.of(
						new MindSessionManager.Participant(OTHER, MindSessionManager.Role.OWNER),
						new MindSessionManager.Participant(HOST, MindSessionManager.Role.TARGET)));

		assertTrue(first.accepted());
		assertFalse(nested.accepted());
		assertEquals(MindSessionManager.Refusal.PARTICIPANT_BUSY, nested.refusal());
		assertEquals(HOST, nested.conflictingParticipant());
		assertEquals(MindSessionManager.Kind.POSSESSION, nested.existingKind());
		assertFalse(MindSessionManager.isParticipating(OTHER));
		assertEquals(FIRST_SESSION, MindSessionManager.activeFor(HOST).orElseThrow().id());
	}

	@Test
	void removingOneSoulLinkTargetKeepsTheOwnerAndOtherTargetAuthoritative() {
		MindSessionManager.acquire(FIRST_SESSION, MindSessionManager.Kind.SOUL_LINK, OWNER,
				List.of(new MindSessionManager.Participant(OWNER, MindSessionManager.Role.OWNER),
						new MindSessionManager.Participant(HOST, MindSessionManager.Role.TARGET),
						new MindSessionManager.Participant(OTHER, MindSessionManager.Role.SHADOW)));

		assertTrue(MindSessionManager.removeParticipant(FIRST_SESSION, HOST));

		assertFalse(MindSessionManager.isParticipating(HOST));
		assertTrue(MindSessionManager.isParticipating(OWNER));
		assertTrue(MindSessionManager.isParticipating(OTHER));
		assertEquals(List.of(OWNER, OTHER), MindSessionManager.session(FIRST_SESSION).orElseThrow()
				.participants().stream().map(MindSessionManager.Participant::id).toList());
	}

	@Test
	void releaseIsIdempotentAndCannotUnlockAnUnrelatedSession() {
		MindSessionManager.acquire(FIRST_SESSION, MindSessionManager.Kind.ASTRAL_PROJECTION, OWNER,
				List.of(new MindSessionManager.Participant(OWNER, MindSessionManager.Role.OWNER)));
		MindSessionManager.acquire(SECOND_SESSION, MindSessionManager.Kind.REALM, OTHER,
				List.of(new MindSessionManager.Participant(OTHER, MindSessionManager.Role.OWNER)));

		assertTrue(MindSessionManager.release(FIRST_SESSION));
		assertFalse(MindSessionManager.release(FIRST_SESSION));
		assertFalse(MindSessionManager.isParticipating(OWNER));
		assertTrue(MindSessionManager.isParticipating(OTHER));
	}

	@Test
	void aSessionHasExactlyOneOwnerRoleAndItMustMatchTheDeclaredOwner() {
		UUID impostor = UUID.fromString("00000000-0000-0000-0000-000000000040");
		MindSessionManager.Admission admission = MindSessionManager.acquire(FIRST_SESSION,
				MindSessionManager.Kind.POSSESSION, OWNER, List.of(
						new MindSessionManager.Participant(OWNER, MindSessionManager.Role.OWNER),
						new MindSessionManager.Participant(impostor, MindSessionManager.Role.OWNER)));

		assertFalse(admission.accepted());
		assertEquals(MindSessionManager.Refusal.INVALID_REQUEST, admission.refusal());
		assertFalse(MindSessionManager.isParticipating(OWNER));
		assertFalse(MindSessionManager.isParticipating(impostor));
	}

	@Test
	void bodyProxyKindsMapToTheExactSessionKind() {
		assertEquals(MindSessionManager.Kind.REALM,
				MindSessionManager.kindFor(BodyProxyKind.REALM));
		assertEquals(MindSessionManager.Kind.ASTRAL_PROJECTION,
				MindSessionManager.kindFor(BodyProxyKind.ASTRAL));
		assertEquals(MindSessionManager.Kind.TELEPORT_MARKING,
				MindSessionManager.kindFor(BodyProxyKind.MARKING));
		assertEquals(MindSessionManager.Kind.POSSESSION,
				MindSessionManager.kindFor(BodyProxyKind.POSSESSION));
		assertEquals(MindSessionManager.Kind.DREAMWALK,
				MindSessionManager.kindFor(BodyProxyKind.DREAMWALK));
	}

	@Test
	void unavailableTargetsUnlinkFromSoulLinkButTerminateControlSessions() {
		MindSessionManager.acquire(FIRST_SESSION, MindSessionManager.Kind.SOUL_LINK, OWNER,
				List.of(new MindSessionManager.Participant(OWNER, MindSessionManager.Role.OWNER),
						new MindSessionManager.Participant(HOST, MindSessionManager.Role.TARGET)));
		assertEquals(MindSessionManager.UnavailableOutcome.REMOVED_PARTICIPANT,
				MindSessionManager.participantUnavailable(HOST));
		assertTrue(MindSessionManager.isParticipating(OWNER));

		MindSessionManager.release(FIRST_SESSION);
		MindSessionManager.acquire(SECOND_SESSION, MindSessionManager.Kind.SOUL_COMPASS, OWNER,
				List.of(new MindSessionManager.Participant(OWNER, MindSessionManager.Role.OWNER),
						new MindSessionManager.Participant(HOST, MindSessionManager.Role.HOST)));
		assertEquals(MindSessionManager.UnavailableOutcome.RELEASED_SESSION,
				MindSessionManager.participantUnavailable(HOST));
		assertFalse(MindSessionManager.isParticipating(OWNER));
	}

	@Test
	void onlyRemoteControlAndViewParticipantsAreBlockedFromOrdinaryMagic() {
		MindSessionManager.acquire(FIRST_SESSION, MindSessionManager.Kind.SOUL_LINK, OWNER,
				List.of(new MindSessionManager.Participant(OWNER, MindSessionManager.Role.OWNER),
						new MindSessionManager.Participant(HOST, MindSessionManager.Role.TARGET)));
		assertFalse(MindSessionManager.blocksOrdinaryMagic(OWNER));
		assertFalse(MindSessionManager.blocksOrdinaryMagic(HOST));

		MindSessionManager.release(FIRST_SESSION);
		MindSessionManager.acquire(SECOND_SESSION, MindSessionManager.Kind.DREAMWALK, OWNER,
				List.of(new MindSessionManager.Participant(OWNER, MindSessionManager.Role.OWNER),
						new MindSessionManager.Participant(HOST, MindSessionManager.Role.HOST)));
		assertTrue(MindSessionManager.blocksOrdinaryMagic(OWNER));
		assertTrue(MindSessionManager.blocksOrdinaryMagic(HOST));
	}
}
