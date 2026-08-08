package com.powers.magic.runtime;

import com.powers.magic.InteractionContext;
import com.powers.magic.MagicActionDefinition;

import java.util.Objects;
import java.util.UUID;

/**
 * Immutable, server-derived facts for one attempted cast. It intentionally
 * contains no client-selected action identity: callers must resolve the held
 * item, spell, crystal mode, or power slot before constructing this record.
 *
 * @param definition canonical action definition
 * @param owner authoritative caster UUID
 * @param dimension authoritative current dimension identity
 * @param anchor authoritative cast origin
 * @param queryRadius maximum distance at which active magic may collide
 * @param gameTime current server-level game tick
 * @param interactionContext server-derived environmental/rank facts
 */
public record MagicCastContext(MagicActionDefinition definition, UUID owner, String dimension,
		PresenceAnchor anchor, double queryRadius, long gameTime,
		InteractionContext interactionContext) {
	/** Validates transaction inputs before any spatial query or resource spend. */
	public MagicCastContext {
		Objects.requireNonNull(definition, "definition");
		Objects.requireNonNull(owner, "owner");
		Objects.requireNonNull(dimension, "dimension");
		Objects.requireNonNull(anchor, "anchor");
		Objects.requireNonNull(interactionContext, "interactionContext");
		if (dimension.isBlank()) throw new IllegalArgumentException("Cast dimension is required");
		if (!Double.isFinite(queryRadius) || queryRadius < 0.0 || queryRadius > 128.0) {
			throw new IllegalArgumentException("Cast query radius must be finite and within 0..128");
		}
		if (gameTime < 0L) throw new IllegalArgumentException("Cast game time cannot be negative");
	}

	/**
	 * Rebinds a delayed cast to its authoritative completion place and tick while
	 * preserving the originally resolved action, owner, range, and environment.
	 */
	public MagicCastContext rebased(String newDimension, PresenceAnchor newAnchor, long newGameTime) {
		return new MagicCastContext(definition, owner, newDimension, newAnchor, queryRadius,
				newGameTime, interactionContext);
	}
}
