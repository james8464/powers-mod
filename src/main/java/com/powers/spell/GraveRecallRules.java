package com.powers.spell;

import java.util.Optional;

/** Pure retention and same-dimension compass rules for owner-only death memories. */
final class GraveRecallRules {
	static final long RETENTION_TICKS = 7L * 24_000L;
	private static final String[] DIRECTIONS = {"south", "south_east", "east", "north_east",
			"north", "north_west", "west", "south_west"};

	private GraveRecallRules() { }

	static boolean retained(long recordedAt, long now) {
		return recordedAt == 0L || recordedAt > 0L && now >= recordedAt && now - recordedAt <= RETENTION_TICKS;
	}

	static Optional<String> bearing(String currentDimension, double currentX, double currentZ,
			String deathDimension, double deathX, double deathZ) {
		if (!currentDimension.equals(deathDimension)) return Optional.empty();
		double dx = deathX - currentX;
		double dz = deathZ - currentZ;
		if (dx * dx + dz * dz < 1.0) return Optional.of("spell.powers.grave_recall.direction.here");
		int octant = Math.floorMod((int) Math.round(Math.atan2(dx, dz) / (Math.PI / 4.0)), 8);
		return Optional.of("spell.powers.grave_recall.direction." + DIRECTIONS[octant]);
	}
}
