package com.powers.item;

/** Decides visual corruption without changing the Rainbow Crystal stack. */
public final class RainbowCrystalVisualRules {
	private RainbowCrystalVisualRules() {
	}

	public static boolean corrupted(boolean rainbowCrystal, boolean darknessHolder) {
		return rainbowCrystal && darknessHolder;
	}
}
