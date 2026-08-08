package com.powers.progression;

import java.util.Objects;

/**
 * One numeric node benefit, optionally scoped to a canonical magic action ID
 * or lower-case aspect name. An empty selector applies to every action.
 */
public record RankPerk(RankPerkType type, double amount, String actionOrAspect) {
	public RankPerk {
		Objects.requireNonNull(type, "type");
		actionOrAspect = actionOrAspect == null ? "" : actionOrAspect.trim().toLowerCase();
		if (!Double.isFinite(amount) || amount < 0) {
			throw new IllegalArgumentException("Rank perk amount must be finite and non-negative");
		}
	}
}
