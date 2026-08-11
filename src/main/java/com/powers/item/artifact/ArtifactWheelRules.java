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

	/** Normalizes live server state into the compact indicators drawn on one segment. */
	public static SegmentStatus segmentStatus(int cost, int cooldownTicks, int cooldownMaximumTicks,
			boolean active, boolean locked, int variant) {
		int pips = cooldownTicks <= 0 || cooldownMaximumTicks <= 0 ? 0
				: (int) Math.clamp(((long) cooldownTicks * SLOT_COUNT
						+ cooldownMaximumTicks - 1L) / cooldownMaximumTicks, 1L, (long) SLOT_COUNT);
		return new SegmentStatus(Math.max(0, cost), pips, active, locked, variant);
	}

	/** Advances an authenticated cooldown snapshot without letting client time underflow it. */
	public static int remainingCooldown(int initialTicks, int elapsedTicks) {
		return Math.max(0, Math.max(0, initialTicks) - Math.max(0, elapsedTicks));
	}

	/** All status information needed for a quick combat decision without opening the library. */
	public record SegmentStatus(int cost, int cooldownPips, boolean active,
			boolean locked, int variant) {
	}
}
