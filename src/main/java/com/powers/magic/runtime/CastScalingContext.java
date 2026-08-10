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
	private static final ThreadLocal<CastSource> SOURCE =
			ThreadLocal.withInitial(() -> CastSource.INNATE);

	private CastScalingContext() {
	}

	/** Returns the adjustment bound to the current server-thread execution. */
	public static CastAdjustment current() {
		return CURRENT.get();
	}

	/** Returns the authoritative invocation route bound to this server operation. */
	public static CastSource currentSource() {
		return SOURCE.get();
	}

	/** Runs an operation with an explicit invocation route and restores nesting safely. */
	public static <T> T withSource(CastSource source, Supplier<T> operation) {
		Objects.requireNonNull(source, "source");
		Objects.requireNonNull(operation, "operation");
		CastSource previous = SOURCE.get();
		SOURCE.set(source);
		try {
			return operation.get();
		} finally {
			if (previous == CastSource.INNATE) SOURCE.remove();
			else SOURCE.set(previous);
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
