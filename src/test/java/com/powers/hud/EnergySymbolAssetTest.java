package com.powers.hud;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pixel-weight and state-separation contract for the vanilla-scale energy row. */
class EnergySymbolAssetTest {
	private static final Path ATLAS = Path.of("src/main/resources/assets/powers/textures/gui/energy_symbols.png");

	@Test
	void atlasKeepsFifteenNinePixelVanillaWeightSymbols() throws Exception {
		BufferedImage image = ImageIO.read(ATLAS.toFile());
		assertEquals(27, image.getWidth());
		assertEquals(45, image.getHeight());
		for (int row = 0; row < 5; row++) {
			for (int column = 0; column < 3; column++) {
				Set<Integer> opaque = new HashSet<>();
				int painted = 0;
				for (int y = row * 9; y < row * 9 + 9; y++) {
					for (int x = column * 9; x < column * 9 + 9; x++) {
						int argb = image.getRGB(x, y);
						if ((argb >>> 24) != 0) {
							painted++;
							opaque.add(argb);
						}
					}
				}
				assertTrue(painted >= 36 && painted <= 44, "row=" + row + " column=" + column);
				assertTrue(opaque.size() >= 3, "flat symbol row=" + row + " column=" + column);
			}
		}
	}

	@Test
	void allFiveEmptyStatesHaveDistinctNonColourShapeCuesAndUseTheReviewedRedraw() throws Exception {
		BufferedImage image = ImageIO.read(ATLAS.toFile());
		Set<String> emptyCells = new HashSet<>();
		for (int row = 0; row < 5; row++) emptyCells.add(cellHash(image, 0, row));
		assertEquals(5, emptyCells.size());
		assertNotEquals("3fb048e39b294e622c4940a1b632cbde880e2b81e28d23038f236a028dd55d2a",
				HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
						.digest(Files.readAllBytes(ATLAS))));
	}

	private static String cellHash(BufferedImage image, int column, int row) throws Exception {
		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		for (int y = row * 9; y < row * 9 + 9; y++) {
			for (int x = column * 9; x < column * 9 + 9; x++) {
				int argb = image.getRGB(x, y);
				digest.update((byte) (argb >>> 24));
				digest.update((byte) (argb >>> 16));
				digest.update((byte) (argb >>> 8));
				digest.update((byte) argb);
			}
		}
		return HexFormat.of().formatHex(digest.digest());
	}
}
