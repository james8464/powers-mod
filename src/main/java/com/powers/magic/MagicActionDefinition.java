package com.powers.magic;

import java.util.Objects;
import java.util.Set;

/**
 * Immutable server-authoritative metadata for one castable or suppressing
 * action. Values here are the unranked baseline; rank profiles produce a
 * separate scaled value object and never mutate the catalogue.
 *
 * @param id stable action identity
 * @param origin registry family that owns the action
 * @param aspects non-empty semantic force set
 * @param delivery spatial/temporal delivery form
 * @param intent gameplay intent
 * @param basePotency unranked abstract strength
 * @param baseRange unranked range in blocks
 * @param baseDurationTicks unranked active duration
 * @param baseEnergy unranked activation energy
 * @param baseCooldownTicks unranked cooldown
 * @param residueTicks post-cast interaction window
 * @param priority unranked interaction priority
 * @param signature audiovisual identity
 * @param significance authored generic-presentation scale
 * @param genericCeremony whether the generic presentation is still required
 * @param targetContract supported entity target/fallback contract
 */
public record MagicActionDefinition(
		MagicActionId id,
		MagicOrigin origin,
		Set<MagicAspect> aspects,
		MagicDelivery delivery,
		MagicIntent intent,
		int basePotency,
		double baseRange,
		int baseDurationTicks,
		int baseEnergy,
		int baseCooldownTicks,
		int residueTicks,
		int priority,
		MagicSignature signature,
		MagicSignificance significance,
		boolean genericCeremony,
		ActionTargetContract targetContract) {
	/** Copies collections and rejects invalid registry data immediately. */
	public MagicActionDefinition {
		Objects.requireNonNull(id, "id");
		Objects.requireNonNull(origin, "origin");
		aspects = Set.copyOf(Objects.requireNonNull(aspects, "aspects"));
		Objects.requireNonNull(delivery, "delivery");
		Objects.requireNonNull(intent, "intent");
		Objects.requireNonNull(signature, "signature");
		Objects.requireNonNull(significance, "significance");
		Objects.requireNonNull(targetContract, "targetContract");
		if (aspects.isEmpty()) {
			throw new IllegalArgumentException("Magic action requires an aspect: " + id);
		}
		if (basePotency < 0 || !Double.isFinite(baseRange) || baseRange < 0.0
				|| baseDurationTicks < 0 || baseEnergy < 0 || baseCooldownTicks < 0
				|| residueTicks < 0 || priority < 0) {
			throw new IllegalArgumentException("Invalid numeric magic action data: " + id);
		}
	}

	/** Returns whether all required mechanical and presentation data is present. */
	public boolean isComplete() {
		return !aspects.isEmpty() && signature.isComplete() && significance != null
				&& targetContract != null;
	}
}
