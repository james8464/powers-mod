package com.powers.magic.runtime;

import java.util.List;
import java.util.Objects;

/** Immutable, side-effect-free interaction result awaiting cast success or rejection. */
public record MagicCastPreview(CastAdjustment adjustment, List<MagicReactionEvent> reactions) {
	public MagicCastPreview {
		Objects.requireNonNull(adjustment, "adjustment");
		reactions = List.copyOf(reactions);
	}

	/** Returns whether gameplay may proceed to payment and execution. */
	public boolean allowed() {
		return adjustment.allowed();
	}
}
