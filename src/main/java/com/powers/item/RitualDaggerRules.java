package com.powers.item;

/** Authoritative Ritual Dagger health payment and energy reward preview. */
public final class RitualDaggerRules {
	public static final float HEALTH_COST = 4.0F;
	public static final float SURVIVAL_FLOOR = 2.0F;
	public static final int ENERGY_REWARD = 80;

	public record Preview(boolean allowed, float healthCost, float resultingHealth,
			int energyGain) {
	}

	private RitualDaggerRules() {
	}

	public static Preview preview(float health, int energy, int capacity) {
		float safeHealth = Math.max(0.0F, health);
		int room = Math.max(0, Math.max(0, capacity) - Math.max(0, energy));
		boolean allowed = safeHealth - HEALTH_COST > SURVIVAL_FLOOR && room > 0;
		return allowed
				? new Preview(true, HEALTH_COST, safeHealth - HEALTH_COST,
						Math.min(ENERGY_REWARD, room))
				: new Preview(false, HEALTH_COST, safeHealth, 0);
	}
}
