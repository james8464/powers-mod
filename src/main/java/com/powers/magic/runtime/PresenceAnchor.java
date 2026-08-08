package com.powers.magic.runtime;

import java.util.UUID;

/**
 * Last server-authoritative position of a presence and the object it follows.
 * Entity-following anchors are refreshed by the runtime tick; the spatial
 * index deliberately stores no live Minecraft entity references.
 *
 * @param kind anchor lifecycle kind
 * @param entityId followed entity, or {@code null} for a fixed location
 * @param x current x coordinate
 * @param y current y coordinate
 * @param z current z coordinate
 */
public record PresenceAnchor(Kind kind, UUID entityId, double x, double y, double z) {
	private static final double MAX_COORDINATE = 60_000_000.0;
	/** Objects to which a presence may be attached. */
	public enum Kind {
		FIXED,
		ENTITY,
		PROJECTILE,
		PLAYER
	}

	/** Validates finite geometry and anchor ownership. */
	public PresenceAnchor {
		if (kind == null) throw new NullPointerException("kind");
		if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
			throw new IllegalArgumentException("Presence coordinates must be finite");
		}
		if (Math.abs(x) > MAX_COORDINATE || Math.abs(y) > MAX_COORDINATE
				|| Math.abs(z) > MAX_COORDINATE) {
			throw new IllegalArgumentException("Presence coordinates exceed the supported world envelope");
		}
		if (kind == Kind.FIXED && entityId != null || kind != Kind.FIXED && entityId == null) {
			throw new IllegalArgumentException("Entity ID does not match presence anchor kind");
		}
	}

	/** Creates an immobile world-position anchor. */
	public static PresenceAnchor fixed(double x, double y, double z) {
		return new PresenceAnchor(Kind.FIXED, null, x, y, z);
	}

	/** Creates an entity-following anchor with its current server position. */
	public static PresenceAnchor entity(Kind kind, UUID entityId, double x, double y, double z) {
		if (kind == Kind.FIXED) throw new IllegalArgumentException("Use fixed() for fixed anchors");
		return new PresenceAnchor(kind, entityId, x, y, z);
	}
}
