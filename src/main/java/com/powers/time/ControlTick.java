package com.powers.time;

/** Monotonic server lifecycle time that continues while vanilla world time is frozen. */
public record ControlTick(long value) {
	public ControlTick {
		if (value < 0L) throw new IllegalArgumentException("Control tick cannot be negative");
	}

	public static ControlTick at(long value) {
		return new ControlTick(value);
	}

	public ControlTick plus(long ticks) {
		if (ticks < 0L) throw new IllegalArgumentException("Control duration cannot be negative");
		return new ControlTick(ticks > Long.MAX_VALUE - value ? Long.MAX_VALUE : value + ticks);
	}

	public long elapsedSince(ControlTick start) {
		return value >= start.value ? value - start.value : 0L;
	}

	public long remainingUntil(ControlTick deadline) {
		return deadline.value >= value ? deadline.value - value : 0L;
	}
}
