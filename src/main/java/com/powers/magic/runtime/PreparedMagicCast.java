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
 * @param preview side-effect-free collision adjustments and pending reactions
 */
public record PreparedMagicCast(MagicCastContext context, MagicCastPreview preview) {
	/** Validates the immutable transaction handle. */
	public PreparedMagicCast {
		Objects.requireNonNull(context, "context");
		Objects.requireNonNull(preview, "preview");
	}

	/** Returns whether payment and execution may proceed. */
	public boolean allowed() {
		return preview.allowed();
	}

	/** Returns the resolved multipliers used during successful gameplay execution. */
	public CastAdjustment adjustment() {
		return preview.adjustment();
	}
}
