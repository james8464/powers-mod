package com.powers.player;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Save-safe coordinates of one player's most recent death. */
public record LastDeathRecord(String dimension, int x, int y, int z, long recordedAt) {
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
}
