package com.powers.spell;

import com.powers.time.WorldTick;

/** Authoritative world-clock cadence for transient spell-field pulses. */
final class SpellFieldTiming {
	private static final int PULSE_INTERVAL_TICKS = 5;

	private SpellFieldTiming() {
	}

	static boolean ready(WorldTick worldTick, WorldTick nextPulseAt) {
		return worldTick.value() >= nextPulseAt.value();
	}

	static WorldTick nextPulseAt(WorldTick worldTick) {
		return worldTick.plus(PULSE_INTERVAL_TICKS);
	}

	/** World-owned fields cannot advance while the server-wide clock is frozen. */
	static boolean mayAdvance(boolean globallyStopped) {
		return !globallyStopped;
	}
}
