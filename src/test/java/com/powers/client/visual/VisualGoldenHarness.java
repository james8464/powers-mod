package com.powers.client.visual;

import com.powers.hud.HudLayout;
import com.powers.hud.HudVisibility;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Deterministic, headless pixel contract for client assets and pure HUD layout.
 * It deliberately uses nearest-neighbour compositing and no host fonts.
 */
public final class VisualGoldenHarness {
	private static final int TILE_WIDTH = 512;
	private static final int TILE_HEIGHT = 288;

	private VisualGoldenHarness() {
	}

	/** One distinct structural HUD input, with Minecraft's scaled logical dimensions. */
	record HudCase(int physicalWidth, int physicalHeight, int guiScale, int heartRows,
			int mountRows, int airRows, boolean armour, boolean spectator,
			int energyRow, int halfUnits) {
		int logicalWidth() {
			return Math.ceilDiv(physicalWidth, guiScale);
		}

		int logicalHeight() {
			return Math.ceilDiv(physicalHeight, guiScale);
		}

		String key() {
			return physicalWidth + "x" + physicalHeight + "@" + guiScale
					+ ":hearts=" + heartRows + ":mount=" + mountRows + ":air=" + airRows
					+ ":armour=" + armour + ":spectator=" + spectator
					+ ":energy=" + energyRow + ":half=" + halfUnits;
		}
	}

	static List<HudCase> hudCases() {
		List<HudCase> cases = new ArrayList<>(53_760);
		for (int guiScale = 1; guiScale <= 4; guiScale++) {
			for (int heartRows = 1; heartRows <= 4; heartRows++) {
				for (int mountRows = 0; mountRows <= 3; mountRows++) {
					for (int airRows = 0; airRows <= 1; airRows++) {
						for (boolean armour : List.of(false, true)) {
							for (boolean spectator : List.of(false, true)) {
								for (int energyRow = 0; energyRow < 5; energyRow++) {
									for (int halfUnits = 0; halfUnits <= 20; halfUnits++) {
										cases.add(new HudCase(1280, 960, guiScale, heartRows,
												mountRows, airRows, armour, spectator, energyRow, halfUnits));
									}
								}
							}
						}
					}
				}
			}
		}
		return List.copyOf(cases);
	}

	public static void main(String[] arguments) throws Exception {
		if (arguments.length != 2 || !(arguments[1].equals("--check")
				|| arguments[1].equals("--update"))) {
			throw new IllegalArgumentException("Usage: VisualGoldenHarness <project-root> --check|--update");
		}
		Path root = Path.of(arguments[0]);
		Path output = root.resolve("docs/verification/goldens");
		Map<String, BufferedImage> images = images(root);
		String manifest = manifest(root, images);
		if (arguments[1].equals("--update")) {
			Files.createDirectories(output);
			for (var entry : images.entrySet()) {
				ImageIO.write(entry.getValue(), "png", output.resolve(entry.getKey()).toFile());
			}
			Files.writeString(output.resolve("manifest.json"), manifest, StandardCharsets.UTF_8);
			return;
		}
		for (var entry : images.entrySet()) compare(output.resolve(entry.getKey()), entry.getValue());
		String committed = Files.readString(output.resolve("manifest.json"), StandardCharsets.UTF_8);
		if (!committed.equals(manifest)) throw new IllegalStateException("Visual golden manifest drift");
	}

	private static Map<String, BufferedImage> images(Path root) throws IOException {
		Map<String, BufferedImage> result = new LinkedHashMap<>();
		result.put("hud-representative-matrix.png", hudMatrix(root));
		return result;
	}

	private static BufferedImage hudMatrix(Path root) throws IOException {
		BufferedImage sheet = canvas(TILE_WIDTH * 4, TILE_HEIGHT * 4, 0xFF111318);
		BufferedImage atlas = asset(root, "textures/gui/energy_symbols.png");
		BufferedImage slot = asset(root, "textures/gui/power_slot.png");
		BufferedImage active = asset(root, "textures/gui/power_slot_active.png");
		for (int index = 0; index < 16; index++) {
			int variant = index % 4;
			HudCase hudCase = new HudCase(1280, 960, index / 4 + 1,
					variant + 1, variant, variant % 2, (variant & 1) != 0,
					index == 15, index % 5, (index * 7) % 21);
			BufferedImage frame = hudFrame(hudCase.logicalWidth(), hudCase.logicalHeight(),
					hudCase.airRows(), hudCase.mountRows(), hudCase.heartRows(), hudCase.armour(),
					hudCase.spectator(), hudCase.energyRow(), hudCase.halfUnits(), atlas, slot, active);
			int x = index % 4 * TILE_WIDTH;
			int y = index / 4 * TILE_HEIGHT;
			drawContained(sheet, frame, x + 6, y + 6, TILE_WIDTH - 12, TILE_HEIGHT - 12);
		}
		return sheet;
	}

	private static BufferedImage hudFrame(int width, int height, int airRows, int mountRows,
			int heartRows, boolean armour, boolean spectator, int stateRow, int halfUnits,
			BufferedImage atlas, BufferedImage slot, BufferedImage active) {
		BufferedImage frame = canvas(width, height, 0xFF1B2028);
		Graphics2D graphics = pixels(frame);
		graphics.setColor(new Color(0xFF27313B, true));
		graphics.fillRect(0, height / 2, width, height / 2);
		int center = width / 2;
		graphics.setColor(new Color(0xC0101010, true));
		graphics.fillRect(center - 91, height - 22, 182, 22);
		for (int row = 0; row < heartRows; row++) {
			for (int pip = 0; pip < 10; pip++) pip(graphics, center - 91 + pip * 8,
					height - 39 - row * 10, 0xFFB72C3B, 0xFF4D111A);
		}
		if (armour) {
			for (int pip = 0; pip < 10; pip++) pip(graphics, center - 91 + pip * 8,
					height - 49 - heartRows * 10, 0xFFD8DEE7, 0xFF58616D);
		}
		if (!spectator) {
			for (int pip = 0; pip < 10; pip++) pip(graphics, center + 10 + pip * 8,
					height - 39, 0xFFC98735, 0xFF5F3714);
			HudLayout layout = HudLayout.forScreen(width, height, airRows, mountRows);
			if (HudVisibility.energy(true, false, false)) {
				for (int symbol = 0; symbol < 10; symbol++) {
					int fill = halfUnits >= (symbol + 1) * 2 ? 2 : halfUnits > symbol * 2 ? 1 : 0;
					int x = HudLayout.energySymbolX(layout.energy(), symbol);
					graphics.drawImage(atlas, x, layout.energy().y(), x + 9, layout.energy().y() + 9,
							fill * 9, stateRow * 9, fill * 9 + 9, stateRow * 9 + 9, null);
				}
			}
			for (int row = 0; row < airRows; row++) {
				for (int pip = 0; pip < 10; pip++) pip(graphics, center + 10 + pip * 8,
						height - 49 - row * 10, 0xFF69C9E8, 0xFF245772);
			}
			for (int row = 0; row < mountRows; row++) {
				for (int pip = 0; pip < 10; pip++) pip(graphics, center + 10 + pip * 8,
						height - 49 - (airRows + row) * 10, 0xFFD69B66, 0xFF653E26);
			}
		}
		HudLayout layout = HudLayout.forScreen(width, height, airRows, mountRows);
		for (int index = 0; index < layout.powerSlots().size(); index++) {
			HudLayout.Rect bounds = layout.powerSlots().get(index);
			BufferedImage texture = index == 0 ? active : slot;
			graphics.drawImage(texture, bounds.x(), bounds.y(), null);
		}
		graphics.dispose();
		return frame;
	}

	private static String manifest(Path root, Map<String, BufferedImage> images) throws Exception {
		StringBuilder json = new StringBuilder("{\n  \"schema\": 2,\n")
				.append("  \"evidenceKind\": \"headless_hud_pixel_contract\",\n")
				.append("  \"hudCombinationsCheckedByHudLayoutTest\": 53760,\n")
				.append("  \"representativeGuiScales\": [1, 2, 3, 4],\n")
				.append("  \"conditions\": [\"extra_hearts\", \"mount\", \"air\", \"armour\", ")
				.append("\"spectator\", \"five_energy_states\", \"twenty_one_half_unit_states\"],\n")
				.append("  \"sourceAssets\": {\n");
		String[] sources = {"textures/gui/energy_symbols.png", "textures/gui/power_slot.png",
				"textures/gui/power_slot_active.png"};
		for (int index = 0; index < sources.length; index++) {
			Path path = assets(root).resolve(sources[index]);
			json.append("    \"").append(sources[index]).append("\": \"").append(hash(path)).append("\"")
					.append(index + 1 == sources.length ? "\n" : ",\n");
		}
		json.append("  },\n  \"goldens\": {\n");
		int index = 0;
		for (var entry : images.entrySet()) {
			json.append("    \"").append(entry.getKey()).append("\": [")
					.append(entry.getValue().getWidth()).append(", ")
					.append(entry.getValue().getHeight()).append("]")
					.append(++index == images.size() ? "\n" : ",\n");
		}
		return json.append("  }\n}\n").toString();
	}

	private static void compare(Path path, BufferedImage generated) throws IOException {
		if (!Files.isRegularFile(path)) throw new IllegalStateException("Missing visual golden: " + path);
		BufferedImage expected = ImageIO.read(path.toFile());
		if (expected.getWidth() != generated.getWidth() || expected.getHeight() != generated.getHeight()) {
			throw new IllegalStateException("Visual golden size drift: " + path);
		}
		for (int y = 0; y < expected.getHeight(); y++) {
			for (int x = 0; x < expected.getWidth(); x++) {
				if (expected.getRGB(x, y) != generated.getRGB(x, y)) {
					throw new IllegalStateException("Visual golden pixel drift: " + path + " at " + x + "," + y);
				}
			}
		}
	}

	private static BufferedImage asset(Path root, String relative) throws IOException {
		return ImageIO.read(assets(root).resolve(relative).toFile());
	}

	private static Path assets(Path root) {
		return root.resolve("src/main/resources/assets/powers");
	}

	private static String hash(Path path) throws Exception {
		return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
				.digest(Files.readAllBytes(path)));
	}

	private static BufferedImage canvas(int width, int height, int argb) {
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		graphics.setColor(new Color(argb, true));
		graphics.fillRect(0, 0, width, height);
		graphics.dispose();
		return image;
	}

	private static Graphics2D pixels(BufferedImage image) {
		Graphics2D graphics = image.createGraphics();
		graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
				RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_OFF);
		return graphics;
	}

	private static void drawContained(BufferedImage destination, BufferedImage source,
			int x, int y, int width, int height) {
		double scale = Math.min((double) width / source.getWidth(), (double) height / source.getHeight());
		int drawnWidth = Math.max(1, (int) Math.floor(source.getWidth() * scale));
		int drawnHeight = Math.max(1, (int) Math.floor(source.getHeight() * scale));
		Graphics2D graphics = pixels(destination);
		graphics.drawImage(source, x + (width - drawnWidth) / 2, y + (height - drawnHeight) / 2,
				drawnWidth, drawnHeight, null);
		graphics.dispose();
	}

	private static void pip(Graphics2D graphics, int x, int y, int fill, int edge) {
		graphics.setColor(new Color(edge, true));
		graphics.fillRect(x, y + 1, 8, 7);
		graphics.setColor(new Color(fill, true));
		graphics.fillRect(x + 1, y + 2, 6, 4);
	}
}
