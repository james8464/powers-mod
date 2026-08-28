package com.powers.client.audio;

import com.powers.audio.LayeredAudioCue;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ClientLayeredAudioStateTest {
	@Test
	void duplicateAndOlderEventIdsAreIgnored() {
		var state = new ClientLayeredAudioState();
		assertTrue(state.acceptEvent(10));
		assertFalse(state.acceptEvent(10));
		assertFalse(state.acceptEvent(9));
		assertEquals(1, state.metrics().acceptedEvents());
		assertEquals(1, state.metrics().duplicateEvents());
		assertEquals(1, state.metrics().staleEvents());
	}

	@Test
	void eventLedgerRetainsOnlyTheNewest256AcceptedIds() {
		var state = new ClientLayeredAudioState();
		for (long eventId = 1; eventId <= 257; eventId++) assertTrue(state.acceptEvent(eventId));
		assertEquals(256, state.metrics().rememberedEvents());
		assertEquals(2, state.metrics().oldestRememberedEventId());
		assertEquals(257, state.metrics().newestEventId());
	}

	@Test
	void duplicateCueAndOriginCellCoalescesInsideFourTickWindow() {
		var state = new ClientLayeredAudioState();
		assertEquals(ClientLayeredAudioState.AdmissionResult.ADMITTED,
				state.admit(LayeredAudioCue.RUNE_HUM, 1.2, 64.0, 2.8, 100).result());
		assertEquals(ClientLayeredAudioState.AdmissionResult.COALESCED,
				state.admit(LayeredAudioCue.RUNE_HUM, 1.9, 64.9, 2.1, 103).result());
		assertEquals(ClientLayeredAudioState.AdmissionResult.ADMITTED,
				state.admit(LayeredAudioCue.RUNE_HUM, 1.2, 64.0, 2.8, 104).result());
		assertEquals(1, state.metrics().coalescedOffers());
	}

	@Test
	void fifthGroupAndNinthGlobalOffersAreDropped() {
		var group = new ClientLayeredAudioState();
		assertAdmitted(group, LayeredAudioCue.AMETHYST_FRACTURE, 0);
		assertAdmitted(group, LayeredAudioCue.BEAM_RING, 1);
		assertAdmitted(group, LayeredAudioCue.BOSS_IMPACT_RING, 2);
		assertAdmitted(group, LayeredAudioCue.WARD_IMPACT, 3);
		assertEquals(ClientLayeredAudioState.AdmissionResult.GROUP_LIMIT,
				group.admit(LayeredAudioCue.INTERACTION_CLASH, 4, 64, 0, 20).result());

		var global = new ClientLayeredAudioState();
		assertAdmitted(global, LayeredAudioCue.AMETHYST_FRACTURE, 0);
		assertAdmitted(global, LayeredAudioCue.BEAM_RING, 1);
		assertAdmitted(global, LayeredAudioCue.BOSS_IMPACT_RING, 2);
		assertAdmitted(global, LayeredAudioCue.WARD_IMPACT, 3);
		assertAdmitted(global, LayeredAudioCue.RUNE_HUM, 4);
		assertAdmitted(global, LayeredAudioCue.CRYSTAL_RESONATE, 5);
		assertAdmitted(global, LayeredAudioCue.TIME_SUSPEND, 6);
		assertAdmitted(global, LayeredAudioCue.RIFT_OPEN, 7);
		assertEquals(ClientLayeredAudioState.AdmissionResult.GLOBAL_LIMIT,
				global.admit(LayeredAudioCue.LIGHT_CHORUS, 8, 64, 0, 20).result());
		assertEquals(1, global.metrics().droppedOffers());
	}

	@Test
	void resetClearsIdentifiersBurstBookkeepingAndCounters() {
		var state = new ClientLayeredAudioState();
		assertTrue(state.acceptEvent(7));
		assertAdmitted(state, LayeredAudioCue.RUNE_HUM, 0);
		state.admit(LayeredAudioCue.RUNE_HUM, 0, 64, 0, 20);
		state.reset();
		assertEquals(ClientLayeredAudioState.Metrics.empty(), state.metrics());
		assertTrue(state.acceptEvent(7));
		assertEquals(ClientLayeredAudioState.AdmissionResult.ADMITTED,
				state.admit(LayeredAudioCue.RUNE_HUM, 0, 64, 0, 20).result());
	}

	private static void assertAdmitted(ClientLayeredAudioState state,
			LayeredAudioCue cue, double x) {
		assertEquals(ClientLayeredAudioState.AdmissionResult.ADMITTED,
				state.admit(cue, x, 64, 0, 20).result());
	}
}
