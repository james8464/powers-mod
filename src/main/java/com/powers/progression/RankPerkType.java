package com.powers.progression;

/**
 * Mechanical dimensions a rank node may influence. Each dimension owns an
 * explicit aggregate cap so freely combining maze paths cannot create
 * unbounded damage, control, resistance, or cooldown loops.
 */
public enum RankPerkType {
	POWER_DAMAGE(0.40),
	HEALING(0.40),
	CONTROL(0.40),
	RANGE(0.35),
	DURATION(0.35),
	ENERGY_CAPACITY(0.50),
	ENERGY_REGEN(0.40),
	ENERGY_COST_REDUCTION(0.25),
	COOLDOWN_REDUCTION(0.25),
	RESISTANCE(0.20),
	MOVEMENT(0.35),
	WARD_INTEGRITY(0.50),
	STEALTH(0.35),
	REVEAL(0.35),
	SOUL(0.40),
	SUMMON(0.35),
	KNOCKBACK(0.35),
	INTERACTION_PRIORITY(0.25),
	BACKLASH_REDUCTION(0.35);

	private final double cap;

	RankPerkType(double cap) {
		this.cap = cap;
	}

	/** Maximum positive contribution of this perk dimension. */
	public double cap() {
		return cap;
	}
}
