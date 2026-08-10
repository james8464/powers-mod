package com.powers.entity;

import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Transient target-side player state used only by Power Test Actors. */
public final class TestActorPowerState {
	public static final int ENERGY_CAPACITY = 1_000;
	private static final Map<UUID, State> STATES = new HashMap<>();

	private record Anchor(String dimensionId, long expiresAt) {
	}

	private record State(int energy, Anchor anchor) {
	}

	private TestActorPowerState() {
	}

	public static int energy(UUID actorId) {
		return STATES.getOrDefault(actorId, new State(ENERGY_CAPACITY, null)).energy();
	}

	/** Removes up to the requested energy and returns the amount actually removed. */
	public static int drain(UUID actorId, int requested) {
		int current = energy(actorId);
		int drained = Math.min(current, Math.max(0, requested));
		setEnergy(actorId, current - drained);
		return drained;
	}

	public static void empty(UUID actorId) {
		setEnergy(actorId, 0);
	}

	public static void restore(UUID actorId) {
		setEnergy(actorId, ENERGY_CAPACITY);
	}

	public static void anchor(UUID actorId, String dimensionId, long expiresAt) {
		State current = state(actorId);
		STATES.put(actorId, new State(current.energy(), new Anchor(dimensionId, expiresAt)));
	}

	/** Returns a valid non-expired dimension ID, clearing invalid state eagerly. */
	public static String anchorDimensionId(UUID actorId, long currentTick) {
		State current = STATES.get(actorId);
		if (current == null || current.anchor() == null) return null;
		Anchor anchor = current.anchor();
		if (currentTick >= anchor.expiresAt() || Identifier.tryParse(anchor.dimensionId()) == null) {
			STATES.put(actorId, new State(current.energy(), null));
			removeDefault(actorId);
			return null;
		}
		return anchor.dimensionId();
	}

	public static void clearAnchor(UUID actorId) {
		State current = STATES.get(actorId);
		if (current == null) return;
		STATES.put(actorId, new State(current.energy(), null));
		removeDefault(actorId);
	}

	public static void clear(UUID actorId) {
		STATES.remove(actorId);
	}

	public static void clearAll() {
		STATES.clear();
	}

	private static State state(UUID actorId) {
		return STATES.getOrDefault(actorId, new State(ENERGY_CAPACITY, null));
	}

	private static void setEnergy(UUID actorId, int energy) {
		State current = state(actorId);
		STATES.put(actorId, new State(Math.clamp(energy, 0, ENERGY_CAPACITY), current.anchor()));
		removeDefault(actorId);
	}

	private static void removeDefault(UUID actorId) {
		State current = STATES.get(actorId);
		if (current != null && current.energy() == ENERGY_CAPACITY && current.anchor() == null) {
			STATES.remove(actorId);
		}
	}
}
