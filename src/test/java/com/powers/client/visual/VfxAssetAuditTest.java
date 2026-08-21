package com.powers.client.visual;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class VfxAssetAuditTest {
	@TempDir
	Path root;

	@Test
	void rejectsUvsOutsideMinecraftElementSpace() throws IOException {
		model("bad_uv", """
				{"textures":{"all":"minecraft:block/stone"},"elements":[
				{"from":[0,0,0],"to":[16,16,16],"faces":{"north":{"uv":[-1,0,16,16],"texture":"#all"}}}]}
				""");
		assertInvalid("UV outside [0,16]");
	}

	@Test
	void rejectsZeroAreaUvs() throws IOException {
		model("zero_uv", """
				{"textures":{"all":"minecraft:block/stone"},"elements":[
				{"from":[0,0,0],"to":[16,16,16],"faces":{"north":{"uv":[4,4,4,12],"texture":"#all"}}}]}
				""");
		assertInvalid("zero-area UV");
	}

	@Test
	void rejectsReversedElementBounds() throws IOException {
		model("reversed", """
				{"textures":{"all":"minecraft:block/stone"},"elements":[
				{"from":[12,0,0],"to":[4,16,16],"faces":{"north":{"texture":"#all"}}}]}
				""");
		assertInvalid("reversed element bounds");
	}

	@Test
	void rejectsUnresolvedTextureVariables() throws IOException {
		model("unresolved", "{" +
				"\"textures\":{\"particle\":\"#missing\"},\"parent\":\"minecraft:block/cube_all\"}");
		assertInvalid("unresolved texture variable #missing");
	}

	@Test
	void rejectsCyclicTextureVariables() throws IOException {
		model("cycle", "{" +
				"\"textures\":{\"a\":\"#b\",\"b\":\"#a\",\"particle\":\"#a\"},"
				+ "\"parent\":\"minecraft:block/cube_all\"}");
		assertInvalid("cyclic texture variable");
	}

	@Test
	void rejectsMalformedDisplayTriples() throws IOException {
		model("short_display", "{" +
				"\"parent\":\"minecraft:item/generated\",\"display\":{\"gui\":{\"rotation\":[0,0]}}}");
		assertInvalid("display triple");
	}

	@Test
	void rejectsNonFiniteDisplayTriples() throws IOException {
		model("infinite_display", "{" +
				"\"parent\":\"minecraft:item/generated\",\"display\":{\"ground\":{\"scale\":[1e309,1,1]}}}");
		assertInvalid("non-finite display value");
	}

	@Test
	void rejectsUnknownNamespacedModelReferences() throws IOException {
		item("unknown_model", "{\"model\":{\"type\":\"minecraft:model\",\"model\":\"powers:item/missing\"}}");
		assertInvalid("unknown model reference powers:item/missing");
	}

	@Test
	void spawnEggRequiresEveryAuthoredRendererContext() throws IOException {
		item("audit_spawn_egg", "{\"model\":{\"type\":\"minecraft:model\",\"model\":\"powers:item/audit_spawn_egg\"}}");
		model("audit_spawn_egg", "{" +
				"\"parent\":\"minecraft:item/generated\",\"textures\":{\"layer0\":\"minecraft:item/egg\"},"
				+ "\"display\":{\"gui\":{\"rotation\":[0,0,0]}}}");
		VfxAssetAudit.AuditManifest manifest = VfxAssetAudit.scan(root);
		IllegalStateException error = assertThrows(IllegalStateException.class,
				() -> manifest.requireLiveCaptures(java.util.Set.of("item/audit_spawn_egg/gui")));
		assertTrue(error.getMessage().contains("missing required spawn-egg display context ground"),
				error::getMessage);
	}

	@Test
	void everyProductionPngOwnsExactPixelEvidenceAndContactSheetPages() {
		VfxAssetAudit.AuditManifest manifest = VfxAssetAudit.scan(Path.of("."));
		List<VfxAssetAudit.AssetEntry> pngs = manifest.assets().stream()
				.filter(asset -> asset.path().endsWith(".png")).toList();
		assertEquals(362, pngs.size(), "the exact production PNG inventory must be reconciled");
		assertFalse(manifest.pageDigests().isEmpty(), "generated contact sheets must be owned by digest");
		long expectedTiles = pngs.stream().mapToLong(asset -> asset.mipResults().size()).sum() * 3L;
		assertEquals(expectedTiles, manifest.pageTiles().size(),
				"every physical frame/mip must appear once on each of three backgrounds");
		for (VfxAssetAudit.AssetEntry png : pngs) {
			assertFalse(png.sheetPageIds().isEmpty(), png.path() + " has no contact-sheet owner");
			assertTrue(png.mipLevels() >= 1, png.path() + " has no complete mip evidence");
			assertEquals(png.frameCount(), png.frameIndices().size(), png.path() + " lost animation timeline indices");
			assertEquals(png.frameCount(), png.frameDurations().size(), png.path() + " lost animation timing");
			assertFalse("STRUCTURAL_ONLY".equals(png.pixelReviewState()), png.path() + " was not pixel-inspected");
			for (String page : png.sheetPageIds()) assertTrue(manifest.pageDigests().containsKey(page),
					() -> png.path() + " cites unowned page " + page);
			for (VfxPixelAudit.MipPixelResult mip : png.mipResults()) for (String background :
					List.of("light", "dark", "checker")) {
				long matches = manifest.pageTiles().stream().filter(tile -> tile.path().equals(png.path())
						&& tile.physicalFrame() == mip.frameIndex() && tile.mipLevel() == mip.mipLevel()
						&& tile.background().equals(background)).count();
				assertEquals(1, matches, png.path() + " frame " + mip.frameIndex() + " mip "
						+ mip.mipLevel() + " lacks exact " + background + " traceability");
			}
		}
	}

	@Test
	void explicitAnimationTimelinePreservesIndicesAndPerFrameDurations() throws IOException {
		Path texture = root.resolve("src/main/resources/assets/powers/textures/item/timeline.png");
		Files.createDirectories(texture.getParent());
		javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(2, 6,
				java.awt.image.BufferedImage.TYPE_INT_ARGB), "png", texture.toFile());
		write("src/main/resources/assets/powers/textures/item/timeline.png.mcmeta", """
				{"animation":{"frametime":4,"frames":[2,2,{"index":0,"time":9},1]}}
				""");
		VfxAssetAudit.AssetEntry entry = VfxAssetAudit.scan(root).assets().stream()
				.filter(asset -> asset.path().endsWith("timeline.png")).findFirst().orElseThrow();
		assertEquals(List.of(2, 2, 0, 1), entry.frameIndices());
		assertEquals(List.of(4, 4, 9, 4), entry.frameDurations());
		assertEquals(2, entry.frameWidth());
		assertEquals(2, entry.frameHeight());
		assertEquals(3, entry.mipResults().stream().map(VfxPixelAudit.MipPixelResult::frameIndex)
				.distinct().count(), "duplicate schedule indices must not duplicate physical source-frame sheets");
	}

	private void model(String name, String json) throws IOException {
		write("src/main/resources/assets/powers/models/item/" + name + ".json", json);
	}

	private void item(String name, String json) throws IOException {
		write("src/main/resources/assets/powers/items/" + name + ".json", json);
	}

	private void write(String relative, String value) throws IOException {
		Path path = root.resolve(relative);
		Files.createDirectories(path.getParent());
		Files.writeString(path, value);
	}

	private void assertInvalid(String expected) {
		IllegalStateException error = assertThrows(IllegalStateException.class,
				() -> VfxAssetAudit.scan(root));
		assertTrue(error.getMessage().contains(expected), error::getMessage);
	}
}
