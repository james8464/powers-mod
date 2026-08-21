package com.powers.client.visual;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VfxPixelAuditTest {
	@TempDir
	Path temporary;

	@Test
	void transparentRgbBesideVisiblePixelsRequiresReview() {
		BufferedImage image = image(2, 1, 0x00FF0000, 0xFF0000FF);
		VfxPixelAudit.PixelEvidence evidence = VfxPixelAudit.inspect(image,
				new VfxPixelAudit.FrameLayout(2, 1, 1));
		assertTrue(evidence.violations().contains("transparent-edge colour contamination"));
	}

	@Test
	void eachAnimationFrameOwnsItsMipChainWithoutCrossFrameBleed() {
		BufferedImage strip = image(2, 4,
				0xFFFF0000, 0xFFFF0000, 0xFFFF0000, 0xFFFF0000,
				0xFF0000FF, 0xFF0000FF, 0xFF0000FF, 0xFF0000FF);
		List<List<BufferedImage>> chains = VfxPixelAudit.buildMipChains(strip,
				new VfxPixelAudit.FrameLayout(2, 2, 2));
		assertEquals(2, chains.size());
		assertEquals(0xFFFF0000, chains.get(0).getLast().getRGB(0, 0));
		assertEquals(0xFF0000FF, chains.get(1).getLast().getRGB(0, 0));
	}

	@Test
	void rejectsAnimationFramesOutsideTheSourceImage() {
		BufferedImage strip = image(3, 4, 0xFFFFFFFF);
		IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
				() -> VfxPixelAudit.inspect(strip, new VfxPixelAudit.FrameLayout(2, 2, 3)));
		assertTrue(error.getMessage().contains("incorrect animation frame bounds"));
	}

	@Test
	void reviewedExceptionIsBoundToExactPathAndDigest() {
		VfxPixelAudit.PixelEvidence evidence = new VfxPixelAudit.PixelEvidence(1, 1, 1,
				List.of("transparent-edge colour contamination"));
		VfxPixelAudit.ReviewedException exception = new VfxPixelAudit.ReviewedException(
				"textures/item/a.png", "abc123", "transparent-edge colour contamination", "authored glow fringe");
		evidence.requireReviewed("textures/item/a.png", "abc123", List.of(exception));
		assertThrows(IllegalStateException.class,
				() -> evidence.requireReviewed("textures/item/a.png", "changed", List.of(exception)));
	}

	@Test
	void pageOwnershipRejectsMissingExtraAndStalePages() throws Exception {
		Path pages = temporary.resolve("pages");
		Files.createDirectories(pages);
		Path expected = pages.resolve("item-001.png");
		ImageIO.write(image(1, 1, 0xFFFFFFFF), "png", expected.toFile());
		String digest = VfxPixelAudit.sha256(expected);
		VfxPixelAudit.verifyOwnedPages(pages, Map.of("item-001.png", digest));
		Files.delete(expected);
		assertThrows(IllegalStateException.class,
				() -> VfxPixelAudit.verifyOwnedPages(pages, Map.of("item-001.png", digest)));
		ImageIO.write(image(1, 1, 0xFF000000), "png", expected.toFile());
		assertThrows(IllegalStateException.class,
				() -> VfxPixelAudit.verifyOwnedPages(pages, Map.of("item-001.png", digest)));
		Path extra = pages.resolve("extra.png");
		ImageIO.write(image(1, 1, 0xFFFFFFFF), "png", extra.toFile());
		assertThrows(IllegalStateException.class,
				() -> VfxPixelAudit.verifyOwnedPages(pages,
						Map.of("item-001.png", VfxPixelAudit.sha256(expected))));
	}

	private static BufferedImage image(int width, int height, int... pixels) {
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		for (int index = 0; index < width * height; index++) {
			image.setRGB(index % width, index / width, pixels.length == 1 ? pixels[0] : pixels[index]);
		}
		return image;
	}
}
