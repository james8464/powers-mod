package com.powers.power.crystals;

import java.util.ArrayList;
import java.util.List;

/** Bounded nearest-first landing search kept inside the realm travel ticket. */
public final class MindscapeArrivalRules {
	private static final int SEARCH_RADIUS = 8;
	private static final List<Offset> OFFSETS = buildOffsets();

	public record Offset(int x, int z) {
	}

	private MindscapeArrivalRules() {
	}

	public static List<Offset> horizontalOffsets() {
		return OFFSETS;
	}

	private static List<Offset> buildOffsets() {
		List<Offset> offsets = new ArrayList<>((SEARCH_RADIUS * 2 + 1) * (SEARCH_RADIUS * 2 + 1));
		for (int radius = 0; radius <= SEARCH_RADIUS; radius++) {
			for (int x = -radius; x <= radius; x++) {
				for (int z = -radius; z <= radius; z++) {
					if (Math.max(Math.abs(x), Math.abs(z)) == radius) offsets.add(new Offset(x, z));
				}
			}
		}
		return List.copyOf(offsets);
	}
}
