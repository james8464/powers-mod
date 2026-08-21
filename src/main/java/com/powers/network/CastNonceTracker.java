package com.powers.network;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

final class CastNonceTracker {
	private record Session(UUID nonce, long expiresAt) { }

	private final long lifetimeTicks;
	private final Map<UUID, Session> sessions = new HashMap<>();

	CastNonceTracker(long lifetimeTicks) {
		this.lifetimeTicks = Math.max(1, lifetimeTicks);
	}

	UUID issue(UUID owner, long currentTick) {
		UUID nonce = UUID.randomUUID();
		sessions.put(owner, new Session(nonce, currentTick + lifetimeTicks));
		return nonce;
	}

	boolean consume(UUID owner, UUID nonce, long currentTick) {
		Session session = sessions.get(owner);
		if (session == null) return false;
		if (currentTick > session.expiresAt()) {
			sessions.remove(owner);
			return false;
		}
		if (!session.nonce().equals(nonce)) return false;
		sessions.remove(owner);
		return true;
	}

	void clear(UUID owner) {
		sessions.remove(owner);
	}

	void clearAll() {
		sessions.clear();
	}

	int size() {
		return sessions.size();
	}

	boolean contains(UUID owner) {
		return sessions.containsKey(owner);
	}
}
