package com.powers.client.audio;

import com.powers.audio.LayeredAudioCue;
import com.powers.audio.LayeredAudioLayer;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ClientLayeredAudioResourcePolicyTest {
	@Test
	void missingOrdinaryLayerFallsBackToRestrainedBaseAndLogsOnce() {
		Predicate<Identifier> onlyBase = id -> id.getPath().equals("sounds/magic/rune_hum.ogg");
		ClientLayeredAudioResourcePolicy.reset();

		var first = ClientLayeredAudioResourcePolicy.resolve(
				LayeredAudioCue.RUNE_HUM, LayeredAudioLayer.MID, false, onlyBase);
		var second = ClientLayeredAudioResourcePolicy.resolve(
				LayeredAudioCue.RUNE_HUM, LayeredAudioLayer.MID, false, onlyBase);

		assertEquals(ClientLayeredAudioResourcePolicy.Mode.BASE, first.mode());
		assertEquals(0.35F, first.gainScale());
		assertTrue(first.logMissing());
		assertFalse(second.logMissing());
	}

	@Test
	void missingReducedCelestialLayerFallsBackToSilence() {
		Predicate<Identifier> onlyBase = id -> id.getPath().equals("sounds/magic/celestial_ring.ogg");
		ClientLayeredAudioResourcePolicy.reset();

		var decision = ClientLayeredAudioResourcePolicy.resolve(LayeredAudioCue.CELESTIAL_RING,
				LayeredAudioLayer.FAR, true, onlyBase);

		assertEquals(ClientLayeredAudioResourcePolicy.Mode.SILENT, decision.mode());
		assertEquals(0.0F, decision.gainScale());
	}
}
