package com.powers.companion;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShadowTaskControllerTest {
	@Test
	void oneForegroundTaskOwnsAndReleasesItsReservation() {
		ShadowTaskController controller = new ShadowTaskController();
		ShadowTask first = ShadowTask.create(ShadowRequest.Kind.ATTACK, "Vessel", 1, 10, 110, 80);
		assertTrue(controller.submit(first).accepted());
		assertFalse(controller.submit(ShadowTask.create(
				ShadowRequest.Kind.SCOUT, "ahead", 1, 11, 50, 0)).accepted());
		assertEquals(80, controller.reservedEnergy());
		ShadowTask.Result cancelled = controller.cancel("owner_stop");
		assertEquals(ShadowTask.State.CANCELLED, cancelled.state());
		assertEquals("owner_stop", cancelled.reason());
		assertEquals(0, controller.reservedEnergy());
	}

	@Test
	void timeoutsAndCompletionReturnExactSaveSafeReasons() {
		ShadowTaskController controller = new ShadowTaskController();
		controller.submit(ShadowTask.create(ShadowRequest.Kind.GET_ITEM,
				"minecraft:torch", 16, 5, 20, 12));
		assertEquals(ShadowTask.State.RUNNING, controller.tick(19).state());
		ShadowTask.Result timeout = controller.tick(20);
		assertEquals(ShadowTask.State.FAILED, timeout.state());
		assertEquals("timeout", timeout.reason());
		assertTrue(controller.active().isEmpty());
		assertTrue(timeout.summary().length() <= ShadowTask.MAX_SUMMARY_LENGTH);
	}
}
