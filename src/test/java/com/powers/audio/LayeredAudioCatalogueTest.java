package com.powers.audio;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class LayeredAudioCatalogueTest {
	@Test
	void allProductionCuesHaveStableNetworkIdentityAndCompleteMetadata() {
		List<String> names = List.of(
				"rune_hum", "crystal_resonate", "amethyst_fracture", "time_suspend",
				"celestial_ring", "beam_ring", "boss_impact_ring", "time_release",
				"rift_open", "rift_close", "soul_tether", "light_chorus",
				"dark_whisper", "ward_impact", "rank_awaken", "interaction_clash");

		assertEquals(16, LayeredAudioCue.values().length);
		for (int networkId = 0; networkId < names.size(); networkId++) {
			LayeredAudioCue cue = LayeredAudioCue.values()[networkId];
			assertEquals(networkId, cue.networkId());
			assertEquals(names.get(networkId), cue.semanticName());
			assertEquals("subtitles.powers." + names.get(networkId), cue.subtitleKey());
			assertEquals(Optional.of(cue), LayeredAudioCue.fromNetworkId(networkId));
			assertEquals(Optional.of(cue), LayeredAudioCue.forSemanticName(names.get(networkId)));
			assertNotNull(cue.profile());
			assertNotNull(cue.group());
			assertFalse(cue.subtitleKey().isBlank());
		}
	}

	@Test
	void unknownNetworkAndSemanticIdentitiesAreRejected() {
		assertEquals(Optional.empty(), LayeredAudioCue.fromNetworkId(-1));
		assertEquals(Optional.empty(), LayeredAudioCue.fromNetworkId(16));
		assertEquals(Optional.empty(), LayeredAudioCue.forSemanticName(null));
		assertEquals(Optional.empty(), LayeredAudioCue.forSemanticName(""));
		assertEquals(Optional.empty(), LayeredAudioCue.forSemanticName("minecraft:rune_hum"));
	}

	@Test
	void onlyCelestialRingIsTinnitusSensitive() {
		for (LayeredAudioCue cue : LayeredAudioCue.values()) {
			assertEquals(cue == LayeredAudioCue.CELESTIAL_RING, cue.tinnitusSensitive());
		}
	}
}
