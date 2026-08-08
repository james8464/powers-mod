package com.powers.power.state;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Owns finite personal ward integrity independently from potion effects. A
 * breaking impact consumes the shield and blocks that final hit; every later
 * hit proceeds normally, preventing resistance-based invulnerability.
 */
public final class MagicShieldManager {
	private static final MagicShieldManager GLOBAL = new MagicShieldManager();
	private static final Impact MISSED = new Impact(false, false, 0.0f, 0);
	private final Map<UUID, ShieldState> shields = new HashMap<>();

	/** Returns the server-thread-owned production shield manager. */
	public static MagicShieldManager global() {
		return GLOBAL;
	}

	/** Replaces an owner's old shield with finite integrity and an absolute expiry tick. */
	public void raise(UUID owner, float integrity, long expiresAt) {
		if (owner == null || !Float.isFinite(integrity) || integrity <= 0 || expiresAt < 0) {
			throw new IllegalArgumentException("Invalid magical shield");
		}
		shields.put(owner, new ShieldState(integrity, integrity, expiresAt));
	}

	/** Consumes integrity for one positive finite impact. */
	public Impact absorb(UUID owner, float damage, long currentTick) {
		if (owner == null || !Float.isFinite(damage) || damage <= 0) return MISSED;
		ShieldState state = live(owner, currentTick);
		if (state == null) return MISSED;
		float remaining = Math.max(0.0f, state.integrity() - damage);
		boolean shattered = remaining <= 0;
		if (shattered) shields.remove(owner);
		else shields.put(owner, new ShieldState(state.maximum(), remaining, state.expiresAt()));
		return new Impact(true, shattered, remaining, fractureStage(state.maximum(), remaining));
	}

	/** Returns whether a non-expired shield remains. */
	public boolean active(UUID owner, long currentTick) {
		return live(owner, currentTick) != null;
	}

	/** Returns 0 intact, 1 cracked, 2 fractured, or 3 absent. */
	public int fractureStage(UUID owner, long currentTick) {
		ShieldState state = live(owner, currentTick);
		return state == null ? 3 : fractureStage(state.maximum(), state.integrity());
	}

	/** Removes one owner's ward explicitly. */
	public void remove(UUID owner) {
		shields.remove(owner);
	}

	/** Expires stale shields and returns the number removed. */
	public int expire(long currentTick) {
		int before = shields.size();
		shields.entrySet().removeIf(entry -> currentTick > entry.getValue().expiresAt());
		return before - shields.size();
	}

	/** Clears every shield at server shutdown. */
	public void clear() {
		shields.clear();
	}

	private ShieldState live(UUID owner, long currentTick) {
		ShieldState state = shields.get(owner);
		if (state != null && currentTick > state.expiresAt()) {
			shields.remove(owner);
			return null;
		}
		return state;
	}

	private static int fractureStage(float maximum, float integrity) {
		double ratio = maximum <= 0 ? 0 : integrity / maximum;
		if (ratio > 0.66) return 0;
		if (ratio > 0.33) return 1;
		return 2;
	}

	/** Result of an attempted impact absorption. */
	public record Impact(boolean blocked, boolean shattered, float integrity, int fractureStage) {
	}

	private record ShieldState(float maximum, float integrity, long expiresAt) {
	}
}
