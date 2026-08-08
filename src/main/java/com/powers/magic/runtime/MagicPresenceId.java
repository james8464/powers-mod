package com.powers.magic.runtime;

import java.util.Objects;
import java.util.UUID;

/** Unique owner token for one active field, projectile, toggle, or residue. */
public record MagicPresenceId(UUID value) {
	/** Rejects missing identifiers at the state boundary. */
	public MagicPresenceId {
		Objects.requireNonNull(value, "value");
	}

	/** Creates a new runtime-only identity. */
	public static MagicPresenceId random() {
		return new MagicPresenceId(UUID.randomUUID());
	}

	@Override
	public String toString() {
		return value.toString();
	}
}
