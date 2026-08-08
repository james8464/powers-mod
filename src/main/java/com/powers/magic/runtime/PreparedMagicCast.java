package com.powers.magic.runtime;

import java.util.Objects;

/**
 * Result of the pre-payment interaction gate. Callers execute the original
 * ability only when {@link #allowed()} is true and call
 * {@link ServerMagicCasts#commit(PreparedMagicCast, net.minecraft.server.level.ServerPlayer)}
 * only after execution
 * reports success.
 *
 * @param context authoritative cast facts
 * @param adjustment resolved collision adjustments
 */
public record PreparedMagicCast(MagicCastContext context, CastAdjustment adjustment) {
	/** Validates the immutable transaction handle. */
	public PreparedMagicCast {
		Objects.requireNonNull(context, "context");
		Objects.requireNonNull(adjustment, "adjustment");
	}

	/** Returns whether payment and execution may proceed. */
	public boolean allowed() {
		return adjustment.allowed();
	}
}
