package com.powers.power.abilities;

import java.util.List;

/** Pure selectable-scale and recurring-cost policy for Size Morphing. */
public final class SizeMorphRules {
	private static final List<Double> SCALES = List.of(
			0.25, 0.5, 0.75, 1.0, 1.25, 1.5, 1.75, 2.0,
			0.125, 2.5, 3.0, 4.0);
	private static final int NORMAL_OPTION = 3;
	private static final int ENERGY_PER_SCALE_UNIT = 4;

	private SizeMorphRules() {
	}

	/** Returns the immutable scale choices exposed by the selection menu. */
	public static List<Double> scales() {
		return SCALES;
	}

	/** Returns the option representing an unmodified player. */
	public static int normalOption() {
		return NORMAL_OPTION;
	}

	/** Rejects packet-selected indices outside the authored scale list. */
	public static boolean isValidOption(int option) {
		return option >= 0 && option < SCALES.size();
	}

	/** Resolves a validated menu option to its exact scale. */
	public static double scale(int option) {
		if (!isValidOption(option)) {
			throw new IllegalArgumentException("Unknown size morph option: " + option);
		}
		return SCALES.get(option);
	}

	/** Preserves the original eight save indices and gates only new extreme forms. */
	public static int minimumRank(int option) {
		if (!isValidOption(option)) throw new IllegalArgumentException(
				"Unknown size morph option: " + option);
		return switch (option) {
			case 8 -> 6;
			case 9 -> 4;
			case 10 -> 7;
			case 11 -> 10;
			default -> 0;
		};
	}

	/** Charges linearly for absolute deviation from normal size. */
	public static int energyDrainPerSecond(double scale) {
		if (!Double.isFinite(scale)) return 0;
		return Math.max(0, (int) Math.round(Math.abs(scale - 1.0) * ENERGY_PER_SCALE_UNIT));
	}
}
