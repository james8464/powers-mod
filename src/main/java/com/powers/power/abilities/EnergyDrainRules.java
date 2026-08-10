package com.powers.power.abilities;

/** Boss-capable but hard-capped vitality conversion for non-player vessels. */
public final class EnergyDrainRules {
	private EnergyDrainRules() {
	}

	public static float mobCompletionDamage(float maximumHealth) {
		return Math.min(1_200.0F, (float) (Math.max(0.0F, maximumHealth) * 0.30D));
	}

	public static float mobPulseDamage(float maximumHealth) {
		return Math.min(40.0F, (float) (Math.max(0.0F, maximumHealth) * 0.02D));
	}
}
