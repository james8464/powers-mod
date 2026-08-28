package com.powers.client.audio;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ClientLayeredAudioMixerSourceTest {
	private static final Path CLIENT_AUDIO = Path.of("src/client/java/com/powers/client/audio");

	@Test
	void mixerPerformsOneColliderClipAndPlaysOneSelectedPositionalEvent() throws Exception {
		String mixer = Files.readString(CLIENT_AUDIO.resolve("ClientLayeredAudioMixer.java"));
		assertEquals(1, occurrences(mixer, "new ClipContext("));
		assertTrue(mixer.contains("ClipContext.Block.COLLIDER"));
		assertTrue(mixer.contains("PowersSounds.layer("));
		assertEquals(1, occurrences(mixer, "new PositionalLayeredSound("));
		assertFalse(mixer.contains("SimpleSoundInstance.forUI"));
		assertFalse(mixer.contains("LayeredAudioLayer.values()"));
	}

	@Test
	void positionalSoundRetainsOriginPlayerCategoryAndLinearAttenuation() throws Exception {
		String sound = Files.readString(CLIENT_AUDIO.resolve("PositionalLayeredSound.java"));
		assertTrue(sound.contains("SoundSource.PLAYERS"));
		assertTrue(sound.contains("Attenuation.LINEAR"));
		assertTrue(sound.contains("this.x = x"));
		assertTrue(sound.contains("this.y = y"));
		assertTrue(sound.contains("this.z = z"));
		assertTrue(sound.contains("this.relative = false"));
	}

	private static int occurrences(String text, String needle) {
		int count = 0;
		for (int at = 0; (at = text.indexOf(needle, at)) >= 0; at += needle.length()) count++;
		return count;
	}
}
