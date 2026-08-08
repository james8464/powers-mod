package com.powers.magic.runtime;

import com.powers.magic.MagicActionId;

import java.util.Objects;
import java.util.UUID;

/**
 * Immutable spatial record for active magic. Expiry uses server game time so
 * pauses and wall-clock changes cannot extend or truncate gameplay state.
 *
 * @param id unique runtime identity
 * @param action canonical magic action
 * @param owner caster/field owner used for cleanup
 * @param dimension namespaced dimension identity
 * @param anchor current spatial anchor
 * @param radius spherical interaction radius in blocks
 * @param expiresAt server game tick at which the presence is no longer active
 */
public record MagicPresence(MagicPresenceId id, MagicActionId action, UUID owner,
		String dimension, PresenceAnchor anchor, double radius, long expiresAt) {
	/** Validates bounded spatial and lifecycle data. */
	public MagicPresence {
		Objects.requireNonNull(id, "id");
		Objects.requireNonNull(action, "action");
		Objects.requireNonNull(owner, "owner");
		Objects.requireNonNull(dimension, "dimension");
		Objects.requireNonNull(anchor, "anchor");
		if (dimension.isBlank()) throw new IllegalArgumentException("Presence dimension is required");
		if (!Double.isFinite(radius) || radius < 0.0 || radius > 128.0) {
			throw new IllegalArgumentException("Presence radius must be finite and within 0..128");
		}
		if (expiresAt < 0L) throw new IllegalArgumentException("Presence expiry cannot be negative");
	}

	/** Returns a copy moved to a newly validated dimension and anchor. */
	public MagicPresence moved(String newDimension, PresenceAnchor newAnchor) {
		return new MagicPresence(id, action, owner, newDimension, newAnchor, radius, expiresAt);
	}

	/** Returns whether this presence has expired at the supplied server tick. */
	public boolean expired(long gameTime) {
		return gameTime >= expiresAt;
	}
}
