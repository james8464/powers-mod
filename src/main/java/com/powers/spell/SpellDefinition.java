package com.powers.spell;

import java.util.Objects;

/** Immutable, data-like spell metadata; all authority remains on the server. */
public record SpellDefinition(
		String id,
		int energyCost,
		int cooldownTicks,
		int channelTicks,
		int requiredRank,
		SpellEffect effect) {
	public SpellDefinition {
		if (id == null || id.isBlank()) throw new IllegalArgumentException("Spell id is required");
		if (energyCost <= 0 || cooldownTicks <= 0 || channelTicks < 0 || requiredRank < 0) {
			throw new IllegalArgumentException("Invalid spell costs or timings: " + id);
		}
		Objects.requireNonNull(effect, "effect");
	}
}
