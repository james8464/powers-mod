package com.powers.item.artifact;

/** Pure hit-testing and keyboard rules for the eight-segment quick wheel. */
public final class ArtifactWheelRules {
	public static final int NONE = -1;
	public static final int CENTER = -2;
	public static final int SLOT_COUNT = 8;
	public static final int INNER_RADIUS = 24;
	public static final int OUTER_RADIUS = 82;

	private ArtifactWheelRules() {
	}

	public static int targetAt(int centerX, int centerY, double mouseX, double mouseY) {
		double dx = mouseX - centerX;
		double dy = mouseY - centerY;
		double distanceSquared = dx * dx + dy * dy;
		if (distanceSquared <= INNER_RADIUS * INNER_RADIUS) return CENTER;
		if (distanceSquared > OUTER_RADIUS * OUTER_RADIUS) return NONE;
		double clockwiseFromNorth = Math.atan2(dx, -dy);
		if (clockwiseFromNorth < 0.0) clockwiseFromNorth += Math.PI * 2.0;
		return (int) Math.floor((clockwiseFromNorth + Math.PI / SLOT_COUNT)
				/ (Math.PI * 2.0 / SLOT_COUNT)) % SLOT_COUNT;
	}

	public static int numberSlot(int key) {
		if (key >= 49 && key <= 56) return key - 49;
		if (key >= 321 && key <= 328) return key - 321;
		return NONE;
	}

	public static boolean isShift(int key) {
		return key == 340 || key == 344;
	}

	public static int releasedSelection(int key, int hoveredTarget) {
		return isShift(key) && hoveredTarget >= 0 && hoveredTarget < SLOT_COUNT
				? hoveredTarget : NONE;
	}
}
