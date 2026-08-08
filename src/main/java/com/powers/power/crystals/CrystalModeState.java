package com.powers.power.crystals;

import com.powers.power.AbilityArithmetic;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Per-player selected convergence mode with bounded wraparound. */
public final class CrystalModeState {
	private final Map<UUID, Integer> modes = new HashMap<>();

	public int current(UUID player, int modeCount) {
		if (modeCount < 1) throw new IllegalArgumentException("A crystal needs at least one mode");
		return Math.floorMod(modes.getOrDefault(player, 0), modeCount);
	}

	public int advance(UUID player, int modeCount) {
		int next = AbilityArithmetic.nextMode(current(player, modeCount), modeCount);
		modes.put(player, next);
		return next;
	}

	public void clear(UUID player) {
		modes.remove(player);
	}

	public void clearAll() {
		modes.clear();
	}
}
