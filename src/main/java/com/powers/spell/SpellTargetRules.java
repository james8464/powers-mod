package com.powers.spell;

/** Pure completion-time validity rules for targets locked before a channel. */
public final class SpellTargetRules {
	private SpellTargetRules() {
	}

	/** A target must preserve every world, sight, life, and range invariant. */
	public static boolean remainsValid(boolean alive, boolean sameDimension, boolean visible,
			double distanceSquared, double maximumRange) {
		return alive && sameDimension && visible && Double.isFinite(distanceSquared)
				&& distanceSquared >= 0.0 && Double.isFinite(maximumRange) && maximumRange >= 0.0
				&& distanceSquared <= maximumRange * maximumRange;
	}
}
