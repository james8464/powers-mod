package com.powers.power.crystals;

/** Pure keyboard and bounds contract for convergence radial selectors. */
public final class CrystalSelectorRules {
	public static final int NONE = -1;

	private CrystalSelectorRules() {
	}

	public static boolean validSelection(int modeCount, int selection) {
		return modeCount > 0 && modeCount <= 8 && selection >= 0 && selection < modeCount;
	}

	public static int numberSlot(int key, int modeCount) {
		int slot = key >= 49 && key <= 56 ? key - 49
				: key >= 321 && key <= 328 ? key - 321 : NONE;
		return validSelection(modeCount, slot) ? slot : NONE;
	}

	public static int targetAt(int centerX, int centerY, double mouseX, double mouseY,
			int modeCount) {
		if (modeCount < 1 || modeCount > 8) return NONE;
		double dx = mouseX - centerX;
		double dy = mouseY - centerY;
		double distance = Math.sqrt(dx * dx + dy * dy);
		if (distance < 28.0 || distance > 86.0) return NONE;
		double angle = Math.atan2(dx, -dy);
		if (angle < 0.0) angle += Math.PI * 2.0;
		return (int) Math.floor((angle + Math.PI / modeCount)
				/ (Math.PI * 2.0 / modeCount)) % modeCount;
	}

	/** Responsive ellipse that leaves clear title, hint, and full-width button lanes. */
	public static Layout layout(int width, int height) {
		int centerX = width / 2;
		int centerY = height / 2;
		int horizontal = Math.clamp(width / 2 - 60, 60, 108);
		int vertical = Math.clamp(height / 2 - 45, 50, 70);
		int buttonWidth = Math.clamp(width / 3, 78, 90);
		int titleY = Math.max(14, centerY - vertical - 42);
		int hintY = Math.min(height - 12, centerY + vertical + 35);
		return new Layout(centerX, centerY, horizontal, vertical, buttonWidth, titleY, hintY);
	}

	public record Layout(int centerX, int centerY, int horizontalRadius, int verticalRadius,
			int buttonWidth, int titleY, int hintY) {
	}
}
