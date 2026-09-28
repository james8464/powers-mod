package com.powers.progression;

import java.util.Objects;

/**
 * One numeric node benefit, optionally scoped to a canonical magic action ID
 * or lower-case aspect name. An empty selector applies to every action.
 */
public record RankPerk(RankPerkType type, double amount, String actionOrAspect) {
	private static final double FOCUS_MULTIPLIER = 1.5;

	public RankPerk {
		Objects.requireNonNull(type, "type");
		actionOrAspect = actionOrAspect == null ? "" : actionOrAspect.trim().toLowerCase();
		if (!Double.isFinite(amount) || amount < 0) {
			throw new IllegalArgumentException("Rank perk amount must be finite and non-negative");
		}
	}

	/** Stable descriptive name used by the maze UI, diagnostics, and generated proof. */
	public String mechanicName() {
		return type.mechanicName();
	}

	/** Executes this node's bounded numerical operation against a supplied baseline. */
	public double execute(double baseline, boolean focused) {
		return type.execute(baseline, amount * (focused ? FOCUS_MULTIPLIER : 1.0));
	}
}
