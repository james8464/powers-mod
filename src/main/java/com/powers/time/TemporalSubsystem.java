package com.powers.time;

/** Explicit clock ownership for every subsystem covered by INT-008. */
public enum TemporalSubsystem {
	LEASE_LIFECYCLE(TemporalClockKind.CONTROL),
	LEASE_DRAIN(TemporalClockKind.CONTROL),
	TIME_STOP_PRESENTATION(TemporalClockKind.CONTROL),
	MAGIC_PRESENCES(TemporalClockKind.WORLD),
	PROJECTILES(TemporalClockKind.WORLD),
	CHANNELS(TemporalClockKind.WORLD),
	SPELL_FIELDS(TemporalClockKind.WORLD),
	CELESTIAL_RUIN(TemporalClockKind.WORLD),
	REALM_CYCLES(TemporalClockKind.WORLD);

	private final TemporalClockKind clock;

	TemporalSubsystem(TemporalClockKind clock) {
		this.clock = clock;
	}

	public TemporalClockKind clock() {
		return clock;
	}
}
