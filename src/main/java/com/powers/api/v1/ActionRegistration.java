package com.powers.api.v1;

import java.util.Objects;
import java.util.Set;

/** Immutable bounded metadata for one extension action in the canonical collision catalogue. */
public record ActionRegistration(String id, CastSource source, Set<ActionAspect> aspects,
		ActionDelivery delivery, ActionIntent intent, int potency, double range,
		int durationTicks, int energyCost, int cooldownTicks, int residueTicks,
		int priority, int primaryRgb) {
	public ActionRegistration {
		Objects.requireNonNull(id, "id");
		Objects.requireNonNull(source, "source");
		aspects = Set.copyOf(Objects.requireNonNull(aspects, "aspects"));
		Objects.requireNonNull(delivery, "delivery");
		Objects.requireNonNull(intent, "intent");
		if (!id.matches("[a-z0-9_.-]{1,64}") || source != CastSource.EXTENSION || aspects.isEmpty()
				|| potency < 0 || !Double.isFinite(range) || range < 0 || range > 128
				|| durationTicks < 0 || energyCost < 0 || cooldownTicks < 0 || residueTicks < 0
				|| priority < 0 || (primaryRgb & 0xFF000000) != 0) {
			throw new IllegalArgumentException("Invalid extension action metadata: " + id);
		}
	}
}
