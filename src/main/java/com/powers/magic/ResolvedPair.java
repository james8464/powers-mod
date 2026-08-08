package com.powers.magic;

import java.util.Objects;

/** One canonical pair and its neutral-context resolution for generated evidence. */
public record ResolvedPair(ActionPair pair, InteractionResolution resolution) {
	/** Validates generated pair data. */
	public ResolvedPair {
		Objects.requireNonNull(pair, "pair");
		Objects.requireNonNull(resolution, "resolution");
	}
}
