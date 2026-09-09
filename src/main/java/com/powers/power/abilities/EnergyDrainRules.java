package com.powers.power.abilities;

/** Boss-capable but hard-capped vitality conversion for non-player vessels. */
public final class EnergyDrainRules {
	public static final int RESISTANCE_TICKS = 8;
	private static final int MAX_MOB_CONVERSION = 120;

	private EnergyDrainRules() {
	}

	public static float mobCompletionDamage(float maximumHealth) {
		return Math.min(1_200.0F, (float) (Math.max(0.0F, maximumHealth) * 0.30D));
	}

	public static float mobPulseDamage(float maximumHealth) {
		return Math.min(40.0F, (float) (Math.max(0.0F, maximumHealth) * 0.02D));
	}

	/** Health-scaled once-per-ritual reservoir, capped independently of target health. */
	public static int mobConversionCap(float maximumHealth) {
		return Math.min(MAX_MOB_CONVERSION,
				Math.max(0, (int) Math.ceil(Math.max(0.0F, maximumHealth) * 0.20D)));
	}

	/** Grants only the unspent part of one ritual's immutable conversion budget. */
	public static int conversionGrant(int requested, int alreadyConverted, int cap) {
		return Math.min(Math.max(0, requested), Math.max(0, cap - Math.max(0, alreadyConverted)));
	}

	/** Holding crouch is a short continuous contest; releasing resets progress. */
	public static int resistanceProgress(int current, boolean resisting) {
		return resisting ? Math.min(RESISTANCE_TICKS, Math.max(0, current) + 1) : 0;
	}

	public static boolean countered(int resistanceProgress) {
		return resistanceProgress >= RESISTANCE_TICKS;
	}

	/** Brightens the tether as the target approaches a successful interruption. */
	public static int tetherStressColor(int resistanceProgress) {
		double progress = Math.clamp((double) resistanceProgress / RESISTANCE_TICKS, 0.0D, 1.0D);
		int red = (int) Math.round(0x7A + (0xE8 - 0x7A) * progress);
		int green = (int) Math.round(0x22 + (0xD7 - 0x22) * progress);
		int blue = (int) Math.round(0xA8 + (0xFF - 0xA8) * progress);
		return red << 16 | green << 8 | blue;
	}
}
