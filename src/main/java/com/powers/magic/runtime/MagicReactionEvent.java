package com.powers.magic.runtime;

import com.powers.magic.InteractionResolution;

import java.util.Objects;

/** One deduplicated reaction ready for server or client presentation. */
public record MagicReactionEvent(MagicCastContext cast, MagicPresence existing,
		InteractionResolution resolution) {
	/** Validates event data at the runtime/presentation boundary. */
	public MagicReactionEvent {
		Objects.requireNonNull(cast, "cast");
		Objects.requireNonNull(existing, "existing");
		Objects.requireNonNull(resolution, "resolution");
	}
}
