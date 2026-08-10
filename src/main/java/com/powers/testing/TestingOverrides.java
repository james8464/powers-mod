package com.powers.testing;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Session-only, operator-controlled limits used for manual power verification. */
public final class TestingOverrides {
	private static final Map<UUID, State> STATES = new HashMap<>();

	public record State(boolean energyDisabled, boolean cooldownsDisabled) {
		public static final State DEFAULT = new State(false, false);
	}

	private TestingOverrides() {
	}

	public static State state(UUID playerId) {
		return STATES.getOrDefault(playerId, State.DEFAULT);
	}

	public static boolean energyDisabled(UUID playerId) {
		return state(playerId).energyDisabled();
	}

	public static boolean cooldownsDisabled(UUID playerId) {
		return state(playerId).cooldownsDisabled();
	}

	public static void setAll(UUID playerId, boolean disabled) {
		setState(playerId, new State(disabled, disabled));
	}

	public static void setEnergyDisabled(UUID playerId, boolean disabled) {
		State current = state(playerId);
		setState(playerId, new State(disabled, current.cooldownsDisabled()));
	}

	public static void setCooldownsDisabled(UUID playerId, boolean disabled) {
		State current = state(playerId);
		setState(playerId, new State(current.energyDisabled(), disabled));
	}

	public static void clear(UUID playerId) {
		STATES.remove(playerId);
	}

	public static void clearAll() {
		STATES.clear();
	}

	/** Returns the post-payment pool without mutating persisted player data. */
	public static int energyAfterCost(int current, int cost, boolean disabled) {
		return disabled ? current : Math.max(0, current - Math.max(0, cost));
	}

	/** Converts a saved recovery into the effective recovery seen by gameplay. */
	public static int cooldownRemaining(int remaining, boolean disabled) {
		return disabled ? 0 : Math.max(0, remaining);
	}

	private static void setState(UUID playerId, State state) {
		if (State.DEFAULT.equals(state)) STATES.remove(playerId);
		else STATES.put(playerId, state);
	}
}
