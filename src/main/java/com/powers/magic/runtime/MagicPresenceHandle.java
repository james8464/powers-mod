package com.powers.magic.runtime;

import java.util.Objects;
import java.util.UUID;

/** Stable lifecycle token tying a runtime presence to its physical manifestation. */
public record MagicPresenceHandle(MagicPresenceId presenceId, Kind kind, UUID boundEntity) {
	public enum Kind {
		ENTITY(true),
		PROJECTILE(true),
		BEAM(false),
		IMPACT(false),
		FIELD(false),
		FORCE_BLOCK(false);

		private final boolean entityBound;

		Kind(boolean entityBound) {
			this.entityBound = entityBound;
		}
	}

	public MagicPresenceHandle {
		Objects.requireNonNull(presenceId, "presenceId");
		Objects.requireNonNull(kind, "kind");
		if (kind.entityBound != (boundEntity != null)) {
			throw new IllegalArgumentException("Presence handle binding does not match its kind");
		}
	}

	public static MagicPresenceHandle entity(MagicPresenceId id, Kind kind, UUID entityId) {
		return new MagicPresenceHandle(id, kind, Objects.requireNonNull(entityId, "entityId"));
	}

	public static MagicPresenceHandle fixed(MagicPresenceId id, Kind kind) {
		return new MagicPresenceHandle(id, kind, null);
	}
}
