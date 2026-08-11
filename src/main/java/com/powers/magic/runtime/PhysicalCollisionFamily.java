package com.powers.magic.runtime;

/** Canonical physical overlap families understood by the live reaction bridge. */
public enum PhysicalCollisionFamily {
	BEAM_BEAM,
	PROJECTILE_PROJECTILE,
	PROJECTILE_FIELD,
	BEAM_FIELD,
	FORCE_BLOCK,
	BODY_FIELD,
	UNSUPPORTED;

	/** Classifies symmetrically so entity tick ordering cannot alter mechanics. */
	public static PhysicalCollisionFamily of(MagicPresenceHandle.Kind first,
			MagicPresenceHandle.Kind second) {
		if (first == MagicPresenceHandle.Kind.BEAM && second == MagicPresenceHandle.Kind.BEAM) {
			return BEAM_BEAM;
		}
		if (first == MagicPresenceHandle.Kind.PROJECTILE
				&& second == MagicPresenceHandle.Kind.PROJECTILE) return PROJECTILE_PROJECTILE;
		if (pair(first, second, MagicPresenceHandle.Kind.PROJECTILE, MagicPresenceHandle.Kind.FIELD)) {
			return PROJECTILE_FIELD;
		}
		if (pair(first, second, MagicPresenceHandle.Kind.BEAM, MagicPresenceHandle.Kind.FIELD)) {
			return BEAM_FIELD;
		}
		if (first == MagicPresenceHandle.Kind.FORCE_BLOCK
				|| second == MagicPresenceHandle.Kind.FORCE_BLOCK) return FORCE_BLOCK;
		if (pair(first, second, MagicPresenceHandle.Kind.ENTITY, MagicPresenceHandle.Kind.FIELD)) {
			return BODY_FIELD;
		}
		return UNSUPPORTED;
	}

	private static boolean pair(MagicPresenceHandle.Kind first, MagicPresenceHandle.Kind second,
			MagicPresenceHandle.Kind expectedFirst, MagicPresenceHandle.Kind expectedSecond) {
		return first == expectedFirst && second == expectedSecond
				|| first == expectedSecond && second == expectedFirst;
	}
}
