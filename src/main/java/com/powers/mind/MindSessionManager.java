package com.powers.mind;

import com.powers.magic.runtime.MagicLifecycleRules;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Sole owner of mutually exclusive mind-affecting session participation.
 * Runtime managers retain their world effects, but no second registry may
 * decide whether a controller, host, linked target, or Shadow is available.
 */
public final class MindSessionManager {
	public static final int MAX_PARTICIPANTS = 16;

	public enum Kind {
		REALM,
		ASTRAL_PROJECTION,
		TELEPORT_MARKING,
		POSSESSION,
		DREAMWALK,
		SOUL_COMPASS,
		SOUL_LINK;

		public String serializedName() {
			return name().toLowerCase(Locale.ROOT);
		}
	}

	public enum Role {
		OWNER,
		HOST,
		TARGET,
		SHADOW
	}

	public enum Refusal {
		NONE,
		INVALID_REQUEST,
		SESSION_ID_IN_USE,
		PARTICIPANT_BUSY
	}

	public enum UnavailableOutcome {
		NONE,
		REMOVED_PARTICIPANT,
		RELEASED_SESSION
	}

	public record Participant(UUID id, Role role) {
		public Participant {
			Objects.requireNonNull(id, "id");
			Objects.requireNonNull(role, "role");
		}
	}

	public record Session(UUID id, Kind kind, UUID ownerId, List<Participant> participants) {
		public Session {
			Objects.requireNonNull(id, "id");
			Objects.requireNonNull(kind, "kind");
			Objects.requireNonNull(ownerId, "ownerId");
			participants = List.copyOf(participants);
		}
	}

	/** Exact, non-sensitive nested-session refusal details for player feedback and diagnostics. */
	public record Admission(boolean accepted, Refusal refusal, UUID conflictingParticipant,
			Kind existingKind) {
		private static Admission acceptedAdmission() {
			return new Admission(true, Refusal.NONE, null, null);
		}

		private static Admission refused(Refusal refusal, UUID participant, Kind existingKind) {
			return new Admission(false, refusal, participant, existingKind);
		}
	}

	private static final Map<UUID, Session> SESSIONS = new LinkedHashMap<>();
	private static final Map<UUID, UUID> SESSION_BY_PARTICIPANT = new LinkedHashMap<>();

	private MindSessionManager() {
	}

	/** Atomically acquires every participant, or leaves both indexes unchanged. */
	public static synchronized Admission acquire(UUID sessionId, Kind kind, UUID ownerId,
			Collection<Participant> requested) {
		Admission preflight = preflight(sessionId, kind, ownerId, requested);
		if (!preflight.accepted()) return preflight;
		List<Participant> participants = List.copyOf(requested);
		Session session = new Session(sessionId, kind, ownerId, participants);
		SESSIONS.put(sessionId, session);
		for (Participant participant : participants) {
			SESSION_BY_PARTICIPANT.put(participant.id(), sessionId);
		}
		return Admission.acceptedAdmission();
	}

	/** Performs the same deterministic validation as acquire without mutating ownership. */
	public static synchronized Admission preflight(UUID sessionId, Kind kind, UUID ownerId,
			Collection<Participant> requested) {
		if (sessionId == null || kind == null || ownerId == null || requested == null
				|| requested.isEmpty() || requested.size() > MAX_PARTICIPANTS
				|| SESSIONS.containsKey(sessionId)) {
			return Admission.refused(sessionId != null && SESSIONS.containsKey(sessionId)
					? Refusal.SESSION_ID_IN_USE : Refusal.INVALID_REQUEST, null, null);
		}
		Map<UUID, Participant> unique = new LinkedHashMap<>();
		int ownerRoles = 0;
		try {
			for (Participant participant : requested) {
				if (participant == null || unique.putIfAbsent(participant.id(), participant) != null) {
					return Admission.refused(Refusal.INVALID_REQUEST, null, null);
				}
				if (participant.role() == Role.OWNER) {
					if (!participant.id().equals(ownerId)) {
						return Admission.refused(Refusal.INVALID_REQUEST, null, null);
					}
					ownerRoles++;
				}
			}
		} catch (NullPointerException ignored) {
			return Admission.refused(Refusal.INVALID_REQUEST, null, null);
		}
		if (ownerRoles != 1) return Admission.refused(Refusal.INVALID_REQUEST, null, null);
		for (Participant participant : unique.values()) {
			UUID existingSessionId = SESSION_BY_PARTICIPANT.get(participant.id());
			if (existingSessionId != null) {
				Session existing = SESSIONS.get(existingSessionId);
				return Admission.refused(Refusal.PARTICIPANT_BUSY, participant.id(),
						existing == null ? null : existing.kind());
			}
		}
		return Admission.acceptedAdmission();
	}

	/** Removes a departed linked target while preserving the rest of its session. */
	public static synchronized boolean removeParticipant(UUID sessionId, UUID participantId) {
		Session session = SESSIONS.get(sessionId);
		if (session == null || participantId == null || participantId.equals(session.ownerId())) return false;
		List<Participant> remaining = session.participants().stream()
				.filter(participant -> !participant.id().equals(participantId)).toList();
		if (remaining.size() == session.participants().size()) return false;
		SESSION_BY_PARTICIPANT.remove(participantId, sessionId);
		SESSIONS.put(sessionId, new Session(session.id(), session.kind(), session.ownerId(), remaining));
		return true;
	}

	/** Applies target-loss cleanup without letting one Soul Link death erase surviving links. */
	public static synchronized UnavailableOutcome participantUnavailable(UUID participantId) {
		Optional<Session> active = activeFor(participantId);
		if (active.isEmpty()) return UnavailableOutcome.NONE;
		Session session = active.get();
		if (session.kind() == Kind.SOUL_LINK && !session.ownerId().equals(participantId)) {
			return removeParticipant(session.id(), participantId)
					? UnavailableOutcome.REMOVED_PARTICIPANT : UnavailableOutcome.NONE;
		}
		return release(session.id()) ? UnavailableOutcome.RELEASED_SESSION : UnavailableOutcome.NONE;
	}

	/** Releases exactly one whole session and is safe to repeat. */
	public static synchronized boolean release(UUID sessionId) {
		Session session = SESSIONS.remove(sessionId);
		if (session == null) return false;
		for (Participant participant : session.participants()) {
			SESSION_BY_PARTICIPANT.remove(participant.id(), sessionId);
		}
		return true;
	}

	public static synchronized Optional<Session> session(UUID sessionId) {
		return Optional.ofNullable(SESSIONS.get(sessionId));
	}

	public static synchronized Optional<Session> activeFor(UUID participantId) {
		UUID sessionId = participantId == null ? null : SESSION_BY_PARTICIPANT.get(participantId);
		return Optional.ofNullable(sessionId == null ? null : SESSIONS.get(sessionId));
	}

	public static synchronized boolean isParticipating(UUID participantId) {
		return participantId != null && SESSION_BY_PARTICIPANT.containsKey(participantId);
	}

	/** Preserves the old no-casting rule only for remote control/view participants. */
	public static synchronized boolean blocksOrdinaryMagic(UUID participantId) {
		Optional<Session> active = activeFor(participantId);
		if (active.isEmpty()) return false;
		return switch (active.get().kind()) {
			case POSSESSION, DREAMWALK, SOUL_COMPASS -> true;
			case REALM, ASTRAL_PROJECTION, TELEPORT_MARKING, SOUL_LINK -> false;
		};
	}

	public static synchronized void clear() {
		SESSIONS.clear();
		SESSION_BY_PARTICIPANT.clear();
	}

	public static Kind kindFor(BodyProxyKind kind) {
		Objects.requireNonNull(kind, "kind");
		return switch (kind) {
			case REALM -> Kind.REALM;
			case ASTRAL -> Kind.ASTRAL_PROJECTION;
			case MARKING -> Kind.TELEPORT_MARKING;
			case POSSESSION -> Kind.POSSESSION;
			case DREAMWALK -> Kind.DREAMWALK;
		};
	}

	/** Maps a session participant to the existing generated lifecycle authority. */
	public static MagicLifecycleRules.Form lifecycleForm(Kind kind, Role role) {
		Objects.requireNonNull(kind, "kind");
		Objects.requireNonNull(role, "role");
		return switch (kind) {
			case REALM -> MagicLifecycleRules.Form.REALM_AVATAR;
			case ASTRAL_PROJECTION -> MagicLifecycleRules.Form.ASTRAL_AVATAR;
			case TELEPORT_MARKING -> MagicLifecycleRules.Form.TELEPORT_MARKER;
			case POSSESSION -> role == Role.OWNER
					? MagicLifecycleRules.Form.POSSESSION_CONTROLLER : MagicLifecycleRules.Form.PHYSICAL;
			case DREAMWALK -> role == Role.OWNER
					? MagicLifecycleRules.Form.DREAMWALK_CONTROLLER : MagicLifecycleRules.Form.PHYSICAL;
			case SOUL_COMPASS -> role == Role.OWNER
					? MagicLifecycleRules.Form.SOUL_COMPASS_CONTROLLER : MagicLifecycleRules.Form.PHYSICAL;
			case SOUL_LINK -> role == Role.OWNER
					? MagicLifecycleRules.Form.SOUL_LINK_OWNER : MagicLifecycleRules.Form.SOUL_LINK_TARGET;
		};
	}
}
