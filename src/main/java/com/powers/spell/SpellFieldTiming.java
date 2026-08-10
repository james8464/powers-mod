package com.powers.spell;

/** Monotonic server-tick cadence for transient spell-field pulses. */
final class SpellFieldTiming {
	private static final int PULSE_INTERVAL_TICKS = 5;

	private SpellFieldTiming() {
	}

	static boolean ready(long serverTick, long nextPulseAt) {
		return serverTick >= nextPulseAt;
	}

	static long nextPulseAt(long serverTick) {
		return serverTick + PULSE_INTERVAL_TICKS;
	}

	/** World-owned fields cannot advance while the server-wide clock is frozen. */
	static boolean mayAdvance(boolean globallyStopped) {
		return !globallyStopped;
	}
}
