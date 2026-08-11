package com.powers.player;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Save-safe coordinates of one player's most recent death. */
public record LastDeathRecord(String dimension, int x, int y, int z) {
	public static final Codec<LastDeathRecord> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.STRING.fieldOf("dimension").forGetter(LastDeathRecord::dimension),
			Codec.INT.fieldOf("x").forGetter(LastDeathRecord::x),
			Codec.INT.fieldOf("y").forGetter(LastDeathRecord::y),
			Codec.INT.fieldOf("z").forGetter(LastDeathRecord::z)
	).apply(instance, LastDeathRecord::new));

	public LastDeathRecord {
		if (dimension == null || dimension.isBlank()) {
			throw new IllegalArgumentException("Death dimension is required");
		}
	}

	public static LastDeathRecord at(String dimension, double x, double y, double z) {
		return new LastDeathRecord(dimension, floor(x), floor(y), floor(z));
	}

	private static int floor(double value) {
		int truncated = (int) value;
		return value < truncated ? truncated - 1 : truncated;
	}
}
