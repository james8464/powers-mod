package com.powers.magic.runtime;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Binds one pre-cast adjustment only while its server-thread execution runs.
 * Nesting and {@code finally} restoration prevent failed casts or exceptions
 * from leaking interaction multipliers into later actions.
 */
public final class CastScalingContext {
	private static final CastAdjustment NEUTRAL = new CastAdjustment(true, 1.0, 1.0, 1.0, List.of());
	private static final ThreadLocal<CastAdjustment> CURRENT = ThreadLocal.withInitial(() -> NEUTRAL);
	private static final CastContext INNATE = CastContext.forSource(CastSource.INNATE);
	private static final ThreadLocal<CastContext> CAST = ThreadLocal.withInitial(() -> INNATE);

	private CastScalingContext() {
	}

	/** Returns the adjustment bound to the current server-thread execution. */
	public static CastAdjustment current() {
		return CURRENT.get();
	}

	/** Returns the authoritative invocation route bound to this server operation. */
	public static CastSource currentSource() {
		return CAST.get().source();
	}

	/** Returns the complete immutable cast/scaling decision for the active server operation. */
	public static CastContext currentCast() {
		return CAST.get();
	}

	/** Runs an operation with an explicit invocation route and restores nesting safely. */
	public static <T> T withSource(CastSource source, Supplier<T> operation) {
		Objects.requireNonNull(source, "source");
		Objects.requireNonNull(operation, "operation");
		CastContext previous = CAST.get();
		CAST.set(CastContext.forSource(source));
		try {
			return operation.get();
		} finally {
			if (previous == INNATE) CAST.remove();
			else CAST.set(previous);
		}
	}

	/** Void overload for source-bound operations. */
	public static void withSource(CastSource source, Runnable operation) {
		withSource(source, () -> {
			operation.run();
			return null;
		});
	}

	/** Runs a value-returning operation with the supplied adjustment. */
	public static <T> T with(CastAdjustment adjustment, Supplier<T> operation) {
		Objects.requireNonNull(adjustment, "adjustment");
		Objects.requireNonNull(operation, "operation");
		CastAdjustment previous = CURRENT.get();
		CURRENT.set(adjustment);
		try {
			return operation.get();
		} finally {
			if (previous == NEUTRAL) CURRENT.remove();
			else CURRENT.set(previous);
		}
	}

	/** Runs a void operation with the supplied adjustment. */
	public static void with(CastAdjustment adjustment, Runnable operation) {
		with(adjustment, () -> {
			operation.run();
			return null;
		});
	}
}
