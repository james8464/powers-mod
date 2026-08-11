package com.powers.force;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Pure geometry for the powered amethyst containment ceremony. */
public final class ForceContainmentRules {
	public static final int RADIUS = 6;
	private static final List<Offset> CARDINAL_CRYSTALS = List.of(
			new Offset(2, 0, 0), new Offset(-2, 0, 0),
			new Offset(0, 0, 2), new Offset(0, 0, -2));
	private static final List<Offset> SPHERE = createSphere();

	private ForceContainmentRules() {
	}

	public static List<Offset> cardinalCrystals() {
		return CARDINAL_CRYSTALS;
	}

	public static List<Offset> sphere() {
		return SPHERE;
	}

	private static List<Offset> createSphere() {
		List<Offset> offsets = new ArrayList<>();
		for (int y = -RADIUS; y <= RADIUS; y++) {
			for (int x = -RADIUS; x <= RADIUS; x++) {
				for (int z = -RADIUS; z <= RADIUS; z++) {
					if (x * x + y * y + z * z <= RADIUS * RADIUS) {
						offsets.add(new Offset(x, y, z));
					}
				}
			}
		}
		offsets.sort(Comparator.comparingInt(Offset::distanceSquared)
				.thenComparingInt(Offset::y).thenComparingInt(Offset::x).thenComparingInt(Offset::z));
		return List.copyOf(offsets);
	}

	public record Offset(int x, int y, int z) {
		int distanceSquared() {
			return x * x + y * y + z * z;
		}
	}
}
