package com.powers.companion;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShadowConjurationManagerTest {
	@Test
	void reservationsCommitOnceOrRefundOnce() {
		var reservation = new ShadowConjurationManager.Reservation(250);
		assertEquals(250, reservation.refund());
		assertEquals(0, reservation.refund());
		assertFalse(reservation.committed());
		var committed = new ShadowConjurationManager.Reservation(40);
		assertTrue(committed.commit());
		assertFalse(committed.commit());
		assertEquals(0, committed.refund());
	}

	@Test
	void riteTimingAndInterruptionAreDeterministic() {
		assertFalse(ShadowConjurationManager.riteComplete(10, 1209));
		assertTrue(ShadowConjurationManager.riteComplete(10, 1210));
		assertTrue(ShadowConjurationManager.interrupts(true, false, false, false));
		assertTrue(ShadowConjurationManager.interrupts(false, true, false, false));
		assertTrue(ShadowConjurationManager.interrupts(false, false, true, false));
		assertTrue(ShadowConjurationManager.interrupts(false, false, false, true));
		assertFalse(ShadowConjurationManager.interrupts(false, false, false, false));
	}
}
