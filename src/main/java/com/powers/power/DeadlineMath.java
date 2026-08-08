package com.powers.power;

/** Math shared by persistent game-time deadlines such as cooldowns and anchors. */
public final class DeadlineMath {
	private DeadlineMath() {
	}

	public static int remainingTicks(long deadline, long now) {
		if (deadline <= now) {
			return 0;
		}
		long remaining = deadline - now;
		return remaining > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) remaining;
	}
}
