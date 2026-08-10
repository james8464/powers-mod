package com.powers.item;

/** Pure validation for nested options submitted by the Shadow Sword menu. */
public final class ShadowSwordSelectionRules {
	private ShadowSwordSelectionRules() {
	}

	/** {@code -1} selects only the action; non-negative values must name a real option. */
	public static boolean validOption(int option, int optionCount) {
		return option == -1 || option >= 0 && option < Math.max(0, optionCount);
	}
}
