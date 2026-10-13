package com.powers.progression;

/**
 * Mechanical dimensions a rank node may influence. Each dimension owns an
 * explicit aggregate cap so freely combining maze paths cannot create
 * unbounded damage, control, resistance, or cooldown loops.
 */
public enum RankPerkType {
	POWER_DAMAGE(0.40, "Potency multiplier", Operation.MULTIPLY_UP),
	HEALING(0.40, "Healing multiplier", Operation.MULTIPLY_UP),
	CONTROL(0.40, "Control-strength multiplier", Operation.MULTIPLY_UP),
	RANGE(0.35, "Reach multiplier", Operation.MULTIPLY_UP),
	DURATION(0.35, "Duration multiplier", Operation.MULTIPLY_UP),
	ENERGY_CAPACITY(0.50, "Energy-capacity multiplier", Operation.MULTIPLY_UP),
	ENERGY_REGEN(0.40, "Energy-regeneration multiplier", Operation.MULTIPLY_UP),
	ENERGY_COST_REDUCTION(0.25, "Energy-cost reduction", Operation.MULTIPLY_DOWN),
	COOLDOWN_REDUCTION(0.25, "Cooldown reduction", Operation.MULTIPLY_DOWN),
	RESISTANCE(0.20, "Damage-resistance multiplier", Operation.MULTIPLY_UP),
	MOVEMENT(0.35, "Movement-speed multiplier", Operation.MULTIPLY_UP),
	WARD_INTEGRITY(0.50, "Ward-integrity multiplier", Operation.MULTIPLY_UP),
	STEALTH(0.35, "Stealth-strength multiplier", Operation.MULTIPLY_UP),
	REVEAL(0.35, "Reveal-strength multiplier", Operation.MULTIPLY_UP),
	SOUL(0.40, "Soul-force multiplier", Operation.MULTIPLY_UP),
	SUMMON(0.35, "Summon-force multiplier", Operation.MULTIPLY_UP),
	KNOCKBACK(0.35, "Knockback multiplier", Operation.MULTIPLY_UP),
	INTERACTION_PRIORITY(0.25, "Interaction-priority bonus", Operation.ADD),
	BACKLASH_REDUCTION(0.35, "Backlash reduction", Operation.MULTIPLY_DOWN);

	private final double cap;
	private final String mechanicName;
	private final Operation operation;

	RankPerkType(double cap, String mechanicName, Operation operation) {
		this.cap = cap;
		this.mechanicName = mechanicName;
		this.operation = operation;
	}

	/** Maximum positive contribution of this perk dimension. */
	public double cap() {
		return cap;
	}

	/** Stable player-facing name for the numerical operation owned by this dimension. */
	public String mechanicName() {
		return mechanicName;
	}

	/** Applies one already-capped contribution to a finite non-negative baseline. */
	public double execute(double baseline, double contribution) {
		if (!Double.isFinite(baseline) || baseline < 0 || !Double.isFinite(contribution)
				|| contribution < 0) {
			throw new IllegalArgumentException("Rank mechanic inputs must be finite and non-negative");
		}
		double bounded = Math.min(cap, contribution);
		return switch (operation) {
			case MULTIPLY_UP -> baseline * (1.0 + bounded);
			case MULTIPLY_DOWN -> baseline * (1.0 - bounded);
			case ADD -> baseline + bounded;
		};
	}

	private enum Operation {
		MULTIPLY_UP,
		MULTIPLY_DOWN,
		ADD
	}
}
