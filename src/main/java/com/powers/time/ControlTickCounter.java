package com.powers.time;

/** Unwraps Minecraft's signed 32-bit server counter into monotonic lifecycle time. */
final class ControlTickCounter {
	private static final long MODULUS = 1L << 32;
	private long epoch;
	private long lastUnsigned = -1L;
	private long lastValue;

	ControlTick observe(int rawTick) {
		long unsigned = Integer.toUnsignedLong(rawTick);
		if (lastUnsigned >= 0L && unsigned < lastUnsigned
				&& lastUnsigned - unsigned > Integer.MAX_VALUE) {
			epoch = epoch > Long.MAX_VALUE - MODULUS ? Long.MAX_VALUE : epoch + MODULUS;
		}
		long candidate = epoch == Long.MAX_VALUE || unsigned > Long.MAX_VALUE - epoch
				? Long.MAX_VALUE : epoch + unsigned;
		lastUnsigned = unsigned;
		lastValue = Math.max(lastValue, candidate);
		return ControlTick.at(lastValue);
	}
}
