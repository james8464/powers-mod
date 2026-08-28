package com.powers.audio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LayeredAudioResourcesTest {
	@Test
	void everyCueAndDistanceLayerProducesItsExactNamespacedEventPath() {
		for (LayeredAudioCue cue : LayeredAudioCue.values()) {
			for (LayeredAudioLayer layer : LayeredAudioLayer.values()) {
				assertEquals(cue.semanticName() + "." + layer.serializedName(),
						cue.eventPath(layer, false));
			}
		}
	}

	@Test
	void onlyCelestialComfortModeProducesDedicatedReducedPaths() {
		for (LayeredAudioLayer layer : LayeredAudioLayer.values()) {
			assertEquals("celestial_ring.reduced." + layer.serializedName(),
					LayeredAudioCue.CELESTIAL_RING.eventPath(layer, true));
			assertEquals("rune_hum." + layer.serializedName(),
					LayeredAudioCue.RUNE_HUM.eventPath(layer, true));
		}
	}
}
