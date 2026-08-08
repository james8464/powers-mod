package com.powers.power.state;

/** Save policy for bounded temporary entities. */
public final class SummonPolicy {
	private SummonPolicy() {
	}

	public static boolean shouldPersist(boolean ephemeral) {
		return !ephemeral;
	}
}
