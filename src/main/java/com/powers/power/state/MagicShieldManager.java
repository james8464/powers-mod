package com.powers.power.state;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Optional;

/**
 * Owns finite personal ward integrity independently from potion effects. A
 * breaking impact consumes the shield and blocks that final hit; every later
 * hit proceeds normally, preventing resistance-based invulnerability.
 */
public final class MagicShieldManager {
	private static final MagicShieldManager GLOBAL = new MagicShieldManager();
	private static final Impact MISSED = new Impact(false, false, 0.0f, 0, false);
	private final Map<UUID, ShieldState> shields = new HashMap<>();

	/** Returns the server-thread-owned production shield manager. */
	public static MagicShieldManager global() {
		return GLOBAL;
	}

	/** Replaces an owner's old shield with finite integrity and an absolute expiry tick. */
	public void raise(UUID owner, float integrity, long expiresAt) {
		raise(owner, integrity, expiresAt, false);
	}

	/** Raises a shield carrying the original caster's authored reflection trait. */
	public void raise(UUID owner, float integrity, long expiresAt, boolean reflective) {
		raise(owner, owner, integrity, expiresAt, reflective);
	}

	/** Repairs a same-caster ward while preserving its target ownership and strongest traits. */
	public void raise(UUID owner, UUID sourceOwner, float integrity, long expiresAt, boolean reflective) {
		if (owner == null || !Float.isFinite(integrity) || integrity <= 0 || expiresAt < 0) {
			throw new IllegalArgumentException("Invalid magical shield");
		}
		ShieldState current = shields.get(owner);
		if (current != null && current.sourceOwner().equals(sourceOwner)) {
			float maximum = Math.max(current.maximum(), integrity);
			float repaired = Math.min(maximum, current.integrity() + integrity);
			shields.put(owner, new ShieldState(sourceOwner, maximum, repaired,
					Math.max(current.expiresAt(), expiresAt), current.reflective() || reflective));
		} else if (current != null) {
			shields.put(owner, new ShieldState(current.sourceOwner(),
					Math.max(current.maximum(), integrity), Math.max(current.integrity(), integrity),
					Math.max(current.expiresAt(), expiresAt), current.reflective() || reflective));
		} else {
			shields.put(owner, new ShieldState(sourceOwner, integrity, integrity, expiresAt, reflective));
		}
	}

	/** Consumes integrity for one positive finite impact. */
	public Impact absorb(UUID owner, float damage, long currentTick) {
		if (owner == null || !Float.isFinite(damage) || damage <= 0) return MISSED;
		ShieldState state = live(owner, currentTick);
		if (state == null) return MISSED;
		float remaining = Math.max(0.0f, state.integrity() - damage);
		boolean shattered = remaining <= 0;
		if (shattered) shields.remove(owner);
		else shields.put(owner, new ShieldState(state.sourceOwner(), state.maximum(), remaining,
				state.expiresAt(), state.reflective()));
		return new Impact(true, shattered, remaining,
				fractureStage(state.maximum(), remaining), state.reflective());
	}

	/** Returns whether a non-expired shield remains. */
	public boolean active(UUID owner, long currentTick) {
		return live(owner, currentTick) != null;
	}

	public Optional<ShieldSnapshot> snapshot(UUID owner, long currentTick) {
		ShieldState state = live(owner, currentTick);
		return state == null ? Optional.empty() : Optional.of(new ShieldSnapshot(state.sourceOwner(),
				state.maximum(), state.integrity(), state.expiresAt(), state.reflective(),
				fractureStage(state.maximum(), state.integrity())));
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
		shields.entrySet().removeIf(entry -> currentTick >= entry.getValue().expiresAt());
		return before - shields.size();
	}

	/**
	 * Returns a stable server-thread snapshot for ambience and diagnostics. This
	 * keeps shield work proportional to raised wards instead of online players.
	 */
	public Set<UUID> activeOwners(long currentTick) {
		expire(currentTick);
		return Set.copyOf(shields.keySet());
	}

	/** Clears every shield at server shutdown. */
	public void clear() {
		shields.clear();
	}

	private ShieldState live(UUID owner, long currentTick) {
		ShieldState state = shields.get(owner);
		if (state != null && currentTick >= state.expiresAt()) {
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
	public record Impact(boolean blocked, boolean shattered, float integrity,
			int fractureStage, boolean reflective) {
	}

	public record ShieldSnapshot(UUID sourceOwner, float maximum, float integrity, long expiresAt,
			boolean reflective, int fractureStage) { }

	private record ShieldState(UUID sourceOwner, float maximum, float integrity, long expiresAt, boolean reflective) {
	}
}
