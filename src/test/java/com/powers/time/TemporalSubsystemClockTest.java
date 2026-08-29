package com.powers.time;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TemporalSubsystemClockTest {
	@Test
	void everyInt008SubsystemDeclaresItsAuthoritativeClock() {
		assertEquals(TemporalClockKind.CONTROL, TemporalSubsystem.LEASE_LIFECYCLE.clock());
		assertEquals(TemporalClockKind.CONTROL, TemporalSubsystem.LEASE_DRAIN.clock());
		assertEquals(TemporalClockKind.CONTROL, TemporalSubsystem.TIME_STOP_PRESENTATION.clock());
		assertEquals(TemporalClockKind.WORLD, TemporalSubsystem.MAGIC_PRESENCES.clock());
		assertEquals(TemporalClockKind.WORLD, TemporalSubsystem.PROJECTILES.clock());
		assertEquals(TemporalClockKind.WORLD, TemporalSubsystem.CHANNELS.clock());
		assertEquals(TemporalClockKind.WORLD, TemporalSubsystem.SPELL_FIELDS.clock());
		assertEquals(TemporalClockKind.WORLD, TemporalSubsystem.CELESTIAL_RUIN.clock());
		assertEquals(TemporalClockKind.WORLD, TemporalSubsystem.REALM_CYCLES.clock());
	}
}
