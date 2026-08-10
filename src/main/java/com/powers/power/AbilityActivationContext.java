package com.powers.power;

import java.util.function.Supplier;

/**
 * Server-thread cast context used by asynchronous abilities to capture the
 * artifact-adjusted cooldown that will be committed after their request is accepted.
 */
public final class AbilityActivationContext {
	private static final ThreadLocal<Integer> COOLDOWN_OVERRIDE = new ThreadLocal<>();

	private AbilityActivationContext() {
	}

	/** Returns the current cast's explicit cooldown, or {@code null} for ordinary casts. */
	public static Integer cooldownOverride() {
		return COOLDOWN_OVERRIDE.get();
	}

	/** Runs one immediate activation with a nest-safe cooldown context. */
	public static <T> T withCooldown(Integer cooldown, Supplier<T> operation) {
		Integer previous = COOLDOWN_OVERRIDE.get();
		if (cooldown == null) COOLDOWN_OVERRIDE.remove();
		else COOLDOWN_OVERRIDE.set(Math.max(0, cooldown));
		try {
			return operation.get();
		} finally {
			if (previous == null) COOLDOWN_OVERRIDE.remove();
			else COOLDOWN_OVERRIDE.set(previous);
		}
	}
}
