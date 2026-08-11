package com.powers.resource;

import net.minecraft.resources.Identifier;

import java.util.Objects;
import java.util.function.Predicate;

/** Selects visible core art when optional pack content is missing or a lookup fails. */
public final class OptionalAssetFallback {
	private OptionalAssetFallback() { }

	public static Identifier resolve(Identifier requested, Identifier visibleFallback,
			Predicate<Identifier> available) {
		Objects.requireNonNull(visibleFallback, "visibleFallback");
		if (requested == null || available == null) return visibleFallback;
		try {
			return available.test(requested) ? requested : visibleFallback;
		} catch (RuntimeException ignored) {
			return visibleFallback;
		}
	}
}
