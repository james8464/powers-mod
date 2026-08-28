package com.powers.client.audio;

import com.powers.audio.LayeredAudioCue;
import com.powers.audio.LayeredAudioLayer;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientLayeredAudioAuditTest {
	@AfterEach
	void reset() {
		ClientLayeredAudioAudit.reset();
	}

	@Test
	void retainsOnlyTheLatest128PrivacySafeJsonlRows() {
		for (int index = 0; index < 130; index++) {
			ClientLayeredAudioAudit.record(new ClientLayeredAudioAudit.Row(
					LayeredAudioCue.RUNE_HUM, LayeredAudioLayer.NEAR, index,
					index % 2 == 0, 0.25F, "admitted",
					false, Identifier.parse("minecraft:overworld"), index, "abcdef1"));
		}

		assertEquals(128, ClientLayeredAudioAudit.rows().size());
		assertEquals(2L, ClientLayeredAudioAudit.rows().getFirst().eventId());
		String json = ClientLayeredAudioAudit.rows().getLast().json();
		assertTrue(json.contains("\"cue\":\"rune_hum\""));
		assertTrue(json.contains("\"subtitleKey\":\"subtitles.powers.rune_hum\""));
		assertTrue(json.contains("\"implementationSha\":\"abcdef1\""));
		assertFalse(json.contains("player"));
		assertFalse(json.contains("path"));
	}

	@Test
	void sanitizesUntrustedResultsAndImplementationIdentifiers() {
		var row = new ClientLayeredAudioAudit.Row(LayeredAudioCue.CELESTIAL_RING,
				LayeredAudioLayer.FAR, 96.0, false, 0.4F, "not allowed",
				true, Identifier.parse("minecraft:the_end"), 7L, "/Users/private");

		assertEquals("dropped", row.result());
		assertEquals("unknown", row.implementationSha());
	}
}
