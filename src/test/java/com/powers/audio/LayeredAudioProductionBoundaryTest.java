package com.powers.audio;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class LayeredAudioProductionBoundaryTest {
	private static final Path MAIN = Path.of("src/main/java");
	private static final Path CLIENT = Path.of("src/client/java");

	@Test
	void centralSoundBoundaryRoutesCatalogueEventsAndPreservesVanillaFallback() throws Exception {
		String source = Files.readString(MAIN.resolve("com/powers/fx/PowerFx.java"));
		assertTrue(source.contains("PowersSounds.fromSound(sound)"));
		assertTrue(source.contains("LayeredAudioService.emit(level, pos"));
		assertTrue(source.contains("level.playSound(null, pos.x, pos.y, pos.z, sound"));
	}

	@Test
	void legacyDistantEventAudioTypesAndDirectBasePlaybackAreAbsent() throws Exception {
		assertFalse(Files.exists(MAIN.resolve("com/powers/network/EventAudioPackets.java")));
		assertFalse(Files.exists(CLIENT.resolve("com/powers/client/fx/ClientEventAudio.java")));
		for (Path source : productionSources()) {
			String text = Files.readString(source);
			assertFalse(text.contains("EventAudioPackets"), source.toString());
			assertFalse(text.matches("(?s).*(?:playSound|playLocalSound)\\([^;]*PowersSounds\\..*"),
					source.toString());
		}
	}

	@Test
	void celestialRingingUsesOneComfortAwareMixerApiAtBothExistingTimings() throws Exception {
		String source = Files.readString(CLIENT.resolve("com/powers/client/fx/ClientCelestialRuinFx.java"));
		assertEquals(2, occurrences(source, "ClientLayeredAudioMixer.playLocalCelestial("));
		assertFalse(source.contains("PowersSounds.CELESTIAL_RING"));
	}

	@Test
	void everyCatalogueCueHasAProductionOrAcceptanceEmissionPath() throws Exception {
		StringBuilder consumers = new StringBuilder();
		for (Path source : productionSources()) {
			String normalized = source.toString().replace('\\', '/');
			if (normalized.endsWith("/PowersSounds.java")
					|| normalized.endsWith("/LayeredAudioCue.java")) continue;
			consumers.append(Files.readString(source)).append('\n');
		}
		for (LayeredAudioCue cue : LayeredAudioCue.values()) {
			assertTrue(consumers.indexOf("PowersSounds." + cue.name()) >= 0
					|| consumers.indexOf('"' + cue.semanticName() + '"') >= 0,
					() -> "No production/acceptance emission path for " + cue);
		}
	}

	private static List<Path> productionSources() throws Exception {
		List<Path> sources = new ArrayList<>();
		for (Path root : List.of(MAIN, CLIENT, Path.of("src/gametest/java"))) {
			if (!Files.isDirectory(root)) continue;
			try (var files = Files.walk(root)) {
				files.filter(path -> path.toString().endsWith(".java")).forEach(sources::add);
			}
		}
		return sources;
	}

	private static int occurrences(String text, String needle) {
		int count = 0;
		for (int at = 0; (at = text.indexOf(needle, at)) >= 0; at += needle.length()) count++;
		return count;
	}
}
