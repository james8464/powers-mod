package com.powers.power.state;

import com.powers.time.ControlTick;
import org.junit.jupiter.api.Test;

import java.util.OptionalLong;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimeStopLeaseRulesTest {
	private static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-000000000001");
	private static final UUID SHADOW = UUID.fromString("00000000-0000-0000-0000-000000000002");

	@Test
	void acquisitionNeverStealsOwnedOrExternalTime() {
		assertTrue(TimeStopLeaseRules.mayAcquire(false, false));
		assertFalse(TimeStopLeaseRules.mayAcquire(true, false));
		assertFalse(TimeStopLeaseRules.mayAcquire(false, true));
	}

	@Test
	void leasesCarrySourceSpecificIdentityAndControlDeadlines() {
		TimeStopLease innate = TimeStopLeaseRules.create(1L, OWNER,
				TimeStopLeaseSource.INNATE, ControlTick.at(100L), Long.MAX_VALUE, null);
		TimeStopLease crystal = TimeStopLeaseRules.create(2L, OWNER,
				TimeStopLeaseSource.CRYSTAL, ControlTick.at(100L), 1_200L, null);
		TimeStopLease shadow = TimeStopLeaseRules.create(3L, OWNER,
				TimeStopLeaseSource.SHADOW, ControlTick.at(100L), Long.MAX_VALUE, SHADOW);

		assertEquals(ControlTick.at(1_300L), crystal.deadline());
		assertFalse(TimeStopLeaseRules.expired(crystal, ControlTick.at(1_299L)));
		assertTrue(TimeStopLeaseRules.expired(crystal, ControlTick.at(1_300L)));
		assertFalse(TimeStopLeaseRules.expired(innate, ControlTick.at(Long.MAX_VALUE)));
		assertEquals(SHADOW, shadow.shadowBody());
		assertThrows(IllegalArgumentException.class, () -> TimeStopLeaseRules.create(4L, OWNER,
				TimeStopLeaseSource.SHADOW, ControlTick.at(0L), Long.MAX_VALUE, null));
	}

	@Test
	void staleOrWrongSourceReleaseCannotTouchAnotherLease() {
		TimeStopLease crystal = TimeStopLeaseRules.create(7L, OWNER,
				TimeStopLeaseSource.CRYSTAL, ControlTick.at(20L), 40L, null);
		assertTrue(TimeStopLeaseRules.matchesRelease(crystal, 7L, OWNER,
				TimeStopLeaseSource.CRYSTAL));
		assertFalse(TimeStopLeaseRules.matchesRelease(crystal, 6L, OWNER,
				TimeStopLeaseSource.CRYSTAL));
		assertFalse(TimeStopLeaseRules.matchesRelease(crystal, 7L, OWNER,
				TimeStopLeaseSource.INNATE));
	}

	@Test
	void externalSupersessionRetiresAuthorityWithoutUnfreezing() {
		TimeStopLease lease = TimeStopLeaseRules.create(9L, OWNER,
				TimeStopLeaseSource.INNATE, ControlTick.at(20L), Long.MAX_VALUE, null);
		assertTrue(TimeStopLeaseRules.shouldUnfreeze(lease, true));
		TimeStopLease superseded = TimeStopLeaseRules.externallySupersede(lease);
		assertTrue(superseded.externallySuperseded());
		assertFalse(TimeStopLeaseRules.shouldUnfreeze(superseded, true));
		assertFalse(TimeStopLeaseRules.shouldUnfreeze(lease, false));
	}

	@Test
	void tokensNeverWrapIntoAnActiveIdentity() {
		assertEquals(OptionalLong.of(1L), TimeStopLeaseRules.nextToken(0L));
		assertEquals(OptionalLong.of(12L), TimeStopLeaseRules.nextToken(11L));
		assertTrue(TimeStopLeaseRules.nextToken(Long.MAX_VALUE).isEmpty());
	}
}
