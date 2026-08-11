package com.powers.power.abilities;

public final class DoubleHealthRules {
	public static final long HEAL_LOCK_TICKS = 200L;
	private DoubleHealthRules() { }
	public static float healToCap(float health, float oldMaximum, float newMaximum) {
		return Math.max(0.0F, Math.min(newMaximum - health, newMaximum - oldMaximum));
	}
	public static boolean mayHeal(long lastHealTick, long now) {
		return now - lastHealTick > HEAL_LOCK_TICKS;
	}
}
