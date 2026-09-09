package com.powers.item;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Structural and pixel-level acceptance contract for the six Arcane Crucible weapon outputs. */
class ConversionWeaponAssetTest {
	private static final Path ASSETS = Path.of("src/main/resources/assets/powers");
	private static final List<String> DARK = List.of(
			"nocturne", "calamity_blade", "revenants_gravecleaver");
	private static final List<String> LIGHT = List.of(
			"solstice", "valhakyra", "zenith");
	private static final Set<String> DISPLAY_CONTEXTS = Set.of(
			"gui", "ground", "fixed", "head",
			"firstperson_righthand", "firstperson_lefthand",
			"thirdperson_righthand", "thirdperson_lefthand");

	@Test
	void everyConversionWeaponHasACompleteModelAndFourFrameAlignmentPulse() throws Exception {
		for (String id : DARK) assertWeapon(id, false);
		for (String id : LIGHT) assertWeapon(id, true);
	}

	private static void assertWeapon(String id, boolean lightAligned) throws Exception {
		Path modelPath = ASSETS.resolve("models/item/" + id + ".json");
		JsonObject model = JsonParser.parseString(Files.readString(modelPath)).getAsJsonObject();
		assertEquals(DISPLAY_CONTEXTS, model.getAsJsonObject("display").keySet(),
				id + " must remain legible in every authored item transform");
		assertTrue(model.getAsJsonArray("elements").size() >= 16,
				id + " must retain authored three-dimensional geometry");

		Path texturePath = ASSETS.resolve("textures/item/" + id + ".png");
		BufferedImage texture = ImageIO.read(texturePath.toFile());
		assertEquals(texture.getWidth() * 4, texture.getHeight(),
				id + " must contain four square animation frames");

		JsonObject metadata = JsonParser.parseString(Files.readString(
				texturePath.resolveSibling(texturePath.getFileName() + ".mcmeta"))).getAsJsonObject();
		JsonObject animation = metadata.getAsJsonObject("animation");
		assertEquals(2, animation.get("frametime").getAsInt());
		JsonArray frames = animation.getAsJsonArray("frames");
		assertEquals(List.of(0, 1, 2, 3, 2, 1), frames.asList().stream()
				.map(frame -> frame.getAsInt()).toList(), id + " must ease into and out of its pulse");

		long changedPixels = 0;
		long baseLuminance = 0;
		long peakLuminance = 0;
		int size = texture.getWidth();
		for (int y = 0; y < size; y++) for (int x = 0; x < size; x++) {
			int base = texture.getRGB(x, y);
			int peak = texture.getRGB(x, y + size * 2);
			if (base != peak) {
				changedPixels++;
				baseLuminance += luminance(base);
				peakLuminance += luminance(peak);
			}
		}
		assertTrue(changedPixels >= 24, id + " pulse must remain visible at item scale");
		assertNotEquals(baseLuminance, peakLuminance, id + " animation frames must be distinct");
		if (lightAligned) {
			assertTrue(peakLuminance > baseLuminance, id + " must crest toward Pure Light");
		} else {
			assertTrue(peakLuminance < baseLuminance, id + " must descend toward Darkness");
		}
	}

	private static int luminance(int argb) {
		return ((argb >>> 16) & 0xff) * 3 + ((argb >>> 8) & 0xff) * 6 + (argb & 0xff);
	}
}
