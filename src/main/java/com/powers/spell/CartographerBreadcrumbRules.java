package com.powers.spell;

import java.util.ArrayList;
import java.util.List;

/** Pure geometry and work bounds for temporary local Cartographer breadcrumbs. */
public final class CartographerBreadcrumbRules {
	private static final int PULSE_COUNT = 10;
	private static final int PULSE_INTERVAL_TICKS = 20;
	private static final int POINTS_PER_PULSE = 4;
	private static final double MAX_LOCAL_DISTANCE = 8.0;

	public record Offset(double x, double z) { }

	private CartographerBreadcrumbRules() {
	}

	public static int pulseCount() {
		return PULSE_COUNT;
	}

	public static int pulseIntervalTicks() {
		return PULSE_INTERVAL_TICKS;
	}

	public static List<Offset> localTrail(long dx, long dz) {
		double length = Math.hypot(dx, dz);
		if (length == 0.0) return List.of();
		double reach = Math.min(length, MAX_LOCAL_DISTANCE);
		List<Offset> points = new ArrayList<>(POINTS_PER_PULSE);
		for (int step = 1; step <= POINTS_PER_PULSE; step++) {
			double distance = reach * step / POINTS_PER_PULSE;
			points.add(new Offset(dx / length * distance, dz / length * distance));
		}
		return List.copyOf(points);
	}
}
