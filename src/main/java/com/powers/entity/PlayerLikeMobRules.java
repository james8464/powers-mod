package com.powers.entity;

/** Pure faction and ranged-cast cadence shared by player-shaped mobs. */
public final class PlayerLikeMobRules {
	public enum Cast { NONE, LIGHTNING, FIREBALL }

	private static final int CAST_INTERVAL = 80;

	private PlayerLikeMobRules() {
	}

	public static boolean mayTarget(boolean hostile, boolean darknessTagged) {
		return hostile && !darknessTagged;
	}

	/** Alternates two readable attacks without evaluating a cast every AI tick. */
	public static Cast castAt(int tick) {
		int phase = Math.floorMod(tick, CAST_INTERVAL * 2);
		if (phase == 0) {
			return Cast.LIGHTNING;
		}
		return phase == CAST_INTERVAL ? Cast.FIREBALL : Cast.NONE;
	}
}
