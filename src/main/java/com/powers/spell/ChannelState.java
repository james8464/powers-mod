package com.powers.spell;

/** Minimal immutable state used to make ritual interruption deterministic and testable. */
public record ChannelState(long finishesAt, double x, double y, double z, String heldItemId, boolean damaged) {
	public ChannelState withDamaged(boolean value) {
		return new ChannelState(finishesAt, x, y, z, heldItemId, value);
	}
}
