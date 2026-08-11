package com.powers.player;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;

/** Save-safe coordinates of one player's most recent death. */
public record LastDeathRecord(String dimension, int x, int y, int z, long recordedAt) {
	public static final long RETENTION_TICKS = 7L * 24_000L;
	private static final String[] DIRECTIONS = {"south", "south_east", "east", "north_east",
			"north", "north_west", "west", "south_west"};
	public static final Codec<LastDeathRecord> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.STRING.fieldOf("dimension").forGetter(LastDeathRecord::dimension),
			Codec.INT.fieldOf("x").forGetter(LastDeathRecord::x),
			Codec.INT.fieldOf("y").forGetter(LastDeathRecord::y),
			Codec.INT.fieldOf("z").forGetter(LastDeathRecord::z),
			Codec.LONG.optionalFieldOf("recorded_at", 0L).forGetter(LastDeathRecord::recordedAt)
	).apply(instance, LastDeathRecord::new));

	/** Legacy construction remains source-compatible; zero is treated as an expired unknown timestamp. */
	public LastDeathRecord(String dimension, int x, int y, int z) {
		this(dimension, x, y, z, 0L);
	}

	public LastDeathRecord {
		if (dimension == null || dimension.isBlank()) {
			throw new IllegalArgumentException("Death dimension is required");
		}
	}

	public static LastDeathRecord at(String dimension, double x, double y, double z) {
		return at(dimension, x, y, z, 0L);
	}

	public static LastDeathRecord at(String dimension, double x, double y, double z, long recordedAt) {
		return new LastDeathRecord(dimension, floor(x), floor(y), floor(z), recordedAt);
	}

	private static int floor(double value) {
		int truncated = (int) value;
		return value < truncated ? truncated - 1 : truncated;
	}

	public boolean retained(long now) {
		return recordedAt == 0L || recordedAt > 0L && now >= recordedAt && now - recordedAt <= RETENTION_TICKS;
	}

	public Optional<String> bearing(String currentDimension, double currentX, double currentZ) {
		if (!currentDimension.equals(dimension)) return Optional.empty();
		double dx = x - currentX, dz = z - currentZ;
		if (dx * dx + dz * dz < 1.0) return Optional.of("spell.powers.grave_recall.direction.here");
		int octant = Math.floorMod((int) Math.round(Math.atan2(dx, dz) / (Math.PI / 4.0)), 8);
		return Optional.of("spell.powers.grave_recall.direction." + DIRECTIONS[octant]);
	}
}
