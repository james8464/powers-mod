package com.powers.spell;

public final class ChannelRules {
	private static final double MAX_MOVEMENT_SQUARED = 0.75 * 0.75;

	private ChannelRules() {
	}

	public static ChannelStatus status(ChannelState state, long now,
			double x, double y, double z, boolean stillHoldingBook, boolean dampened) {
		double dx = x - state.x();
		double dy = y - state.y();
		double dz = z - state.z();
		if (state.damaged() || !stillHoldingBook || dampened
				|| dx * dx + dy * dy + dz * dz > MAX_MOVEMENT_SQUARED) {
			return ChannelStatus.INTERRUPTED;
		}
		return now >= state.finishesAt() ? ChannelStatus.COMPLETE : ChannelStatus.CHANNELING;
	}
}
