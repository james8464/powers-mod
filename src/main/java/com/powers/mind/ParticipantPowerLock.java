package com.powers.mind;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Prevents either side of a mind session from starting or receiving another power session. */
public final class ParticipantPowerLock {
	private static final int MAX_PARTICIPANTS = 16;
	private static final Map<UUID, UUID> SESSION_BY_PARTICIPANT = new HashMap<>();
	private static final Map<UUID, Set<UUID>> PARTICIPANTS_BY_SESSION = new HashMap<>();

	private ParticipantPowerLock() {
	}

	/** Atomically acquires every participant, or leaves the lock table unchanged. */
	public static synchronized boolean acquire(UUID session, Collection<UUID> participants) {
		Objects.requireNonNull(session, "session");
		Objects.requireNonNull(participants, "participants");
		Set<UUID> unique = new HashSet<>(participants);
		if (unique.isEmpty() || unique.size() != participants.size()
				|| unique.size() > MAX_PARTICIPANTS || unique.contains(null)
				|| PARTICIPANTS_BY_SESSION.containsKey(session)) return false;
		for (UUID participant : unique) {
			if (SESSION_BY_PARTICIPANT.containsKey(participant)) return false;
		}
		Set<UUID> immutable = Set.copyOf(unique);
		PARTICIPANTS_BY_SESSION.put(session, immutable);
		for (UUID participant : immutable) SESSION_BY_PARTICIPANT.put(participant, session);
		return true;
	}

	public static synchronized boolean isLocked(UUID participant) {
		return participant != null && SESSION_BY_PARTICIPANT.containsKey(participant);
	}

	/** Releases one complete session without disturbing unrelated locks. */
	public static synchronized void release(UUID session) {
		Set<UUID> participants = PARTICIPANTS_BY_SESSION.remove(session);
		if (participants == null) return;
		for (UUID participant : participants) SESSION_BY_PARTICIPANT.remove(participant, session);
	}

	/** Lifecycle-only cleanup for server shutdown and isolated tests. */
	public static synchronized void clear() {
		SESSION_BY_PARTICIPANT.clear();
		PARTICIPANTS_BY_SESSION.clear();
	}
}
