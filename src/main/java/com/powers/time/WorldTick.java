package com.powers.time;

/** World gameplay time that stops whenever vanilla freezes server simulation. */
public record WorldTick(long value) {
	public WorldTick {
		if (value < 0L) throw new IllegalArgumentException("World tick cannot be negative");
	}

	public static WorldTick at(long value) {
		return new WorldTick(value);
	}

	public WorldTick plus(long ticks) {
		if (ticks < 0L) throw new IllegalArgumentException("World duration cannot be negative");
		return new WorldTick(ticks > Long.MAX_VALUE - value ? Long.MAX_VALUE : value + ticks);
	}

	public long elapsedSince(WorldTick start) {
		return value >= start.value ? value - start.value : 0L;
	}

	public long remainingUntil(WorldTick deadline) {
		return deadline.value >= value ? deadline.value - value : 0L;
	}
}
