package com.powers.client.visual;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Prevents headless texture composites from being presented as production renderer evidence. */
class VfxCoverageContractTest {
	private static final Path ROOT = Path.of(System.getProperty("user.dir"));

	@Test
	void historicalGoldenManifestClaimsOnlyItsHudPixelContract() throws IOException {
		JsonObject manifest = JsonParser.parseString(Files.readString(ROOT.resolve(
				"docs/verification/goldens/manifest.json"))).getAsJsonObject();
		Set<String> goldens = manifest.getAsJsonObject("goldens").keySet();
		assertEquals(Set.of("hud-representative-matrix.png"), goldens,
				"raw textures and synthetic skies are not renderer proof");
		assertEquals(Set.of("textures/gui/energy_symbols.png", "textures/gui/power_slot.png",
				"textures/gui/power_slot_active.png"), manifest.getAsJsonObject("sourceAssets").keySet(),
				"the historical harness may cite only assets it renders into its HUD contract");
		String serialized = manifest.toString();
		assertFalse(serialized.contains("reduced_motion"),
				"the headless harness has no reduced-motion input");
		assertFalse(serialized.matches(".*(?:screen-surface|entity-uv|spawn-eggs|sky-contract).*"),
				"renderer claims require real client capture IDs");
		assertEquals(53_760, manifest.get("hudCombinationsCheckedByHudLayoutTest").getAsInt());
	}

	@Test
	void finalMatrixDescribesStructuralEvidenceWithoutTheStale160Count() throws IOException {
		String row = Files.readAllLines(ROOT.resolve("docs/verification/final-requirement-matrix.md"))
				.stream().filter(line -> line.startsWith("| R03 ")).findFirst().orElseThrow();
		assertFalse(row.contains("160 decoded"));
		assertTrue(row.contains("970 exact-identity"));
		assertTrue(row.contains("structural evidence only"));
	}

	@Test
	void hudGalleryRequiresAuthoritativeStateReadinessBetweenMotionHalves() throws IOException {
		String source = Files.readString(ROOT.resolve(
				"src/gametest/java/com/powers/client/VfxUiGallery.java"));
		assertTrue(source.contains("DISMOUNT_OVERLAY_SETTLE_TICKS"),
				"the retained vanilla dismount overlay needs a named bounded settle window");
		assertTrue(source.contains("awaitHudState(context, false, false)"),
				"ordinary HUD captures must observe survival and no vehicle on the client");
		assertTrue(source.contains("awaitHudState(context, true, false)"),
				"spectator proof must observe spectator mode after dismount");
		assertTrue(source.contains("awaitHudState(context, false, true)"),
				"mount proof must observe an authoritative client vehicle");
	}
}
