package com.powers.client.visual;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Deterministic pixel evidence that deliberately makes no renderer-level claims. */
public final class VfxPixelAudit {
	private static final String EDGE_CONTAMINATION = "transparent-edge colour contamination";

	private VfxPixelAudit() {
	}

	public static PixelEvidence inspect(BufferedImage image, FrameLayout layout) {
		return inspectFrames(image, splitFrames(image, layout));
	}

	public static PixelEvidence inspect(BufferedImage image, AnimationLayout layout) {
		return inspectFrames(image, splitFrames(image, layout));
	}

	private static PixelEvidence inspectFrames(BufferedImage image, List<BufferedImage> frames) {
		Set<String> violations = new LinkedHashSet<>();
		List<MipPixelResult> mipResults = new ArrayList<>();
		for (int frameIndex = 0; frameIndex < frames.size(); frameIndex++) {
			BufferedImage frame = frames.get(frameIndex);
			int level = 0;
			BufferedImage mip = frame;
			do {
				long transparent = 0;
				long translucent = 0;
				long opaque = 0;
				for (int y = 0; y < mip.getHeight(); y++) for (int x = 0; x < mip.getWidth(); x++) {
					int alpha = mip.getRGB(x, y) >>> 24;
					if (alpha == 0) transparent++;
					else if (alpha == 255) opaque++;
					else translucent++;
				}
				mipResults.add(new MipPixelResult(frameIndex, level, mip.getWidth(), mip.getHeight(),
						transparent, translucent, opaque));
				if (mip.getWidth() == 1 && mip.getHeight() == 1) break;
				mip = downsample(mip);
				level++;
			} while (true);
			for (int y = 0; y < frame.getHeight(); y++) {
				for (int x = 0; x < frame.getWidth(); x++) {
					int pixel = frame.getRGB(x, y);
					if ((pixel >>> 24) == 0 && (pixel & 0x00FFFFFF) != 0 && touchesVisible(frame, x, y)) {
						violations.add(EDGE_CONTAMINATION);
					}
				}
			}
		}
		return new PixelEvidence(image.getWidth(), image.getHeight(), frames.size(),
				List.copyOf(violations), List.copyOf(mipResults));
	}

	public static List<List<BufferedImage>> buildMipChains(BufferedImage image, FrameLayout layout) {
		return buildMipChains(splitFrames(image, layout));
	}

	public static List<List<BufferedImage>> buildMipChains(BufferedImage image, AnimationLayout layout) {
		return buildMipChains(splitFrames(image, layout));
	}

	private static List<List<BufferedImage>> buildMipChains(List<BufferedImage> frames) {
		List<List<BufferedImage>> result = new ArrayList<>();
		for (BufferedImage frame : frames) {
			List<BufferedImage> chain = new ArrayList<>();
			chain.add(frame);
			while (frame.getWidth() > 1 || frame.getHeight() > 1) {
				frame = downsample(frame);
				chain.add(frame);
			}
			result.add(List.copyOf(chain));
		}
		return List.copyOf(result);
	}

	private static List<BufferedImage> splitFrames(BufferedImage image, AnimationLayout layout) {
		if (layout.frameWidth() <= 0 || layout.frameHeight() <= 0
				|| image.getWidth() % layout.frameWidth() != 0 || image.getHeight() % layout.frameHeight() != 0) {
			throw new IllegalArgumentException("incorrect animation frame bounds: source does not contain an exact frame grid");
		}
		int columns = image.getWidth() / layout.frameWidth();
		int rows = image.getHeight() / layout.frameHeight();
		int physicalFrames = columns * rows;
		if (layout.frameIndices().isEmpty()) throw new IllegalArgumentException("incorrect animation frame bounds: empty timeline");
		for (int index : layout.frameIndices()) if (index < 0 || index >= physicalFrames) {
			throw new IllegalArgumentException("incorrect animation frame bounds: frame index " + index
					+ " outside 0.." + (physicalFrames - 1));
		}
		List<BufferedImage> frames = new ArrayList<>();
		for (int index = 0; index < physicalFrames; index++) {
			int x = index % columns * layout.frameWidth();
			int y = index / columns * layout.frameHeight();
			BufferedImage copy = new BufferedImage(layout.frameWidth(), layout.frameHeight(), BufferedImage.TYPE_INT_ARGB);
			for (int row = 0; row < layout.frameHeight(); row++) for (int column = 0; column < layout.frameWidth(); column++) {
				copy.setRGB(column, row, image.getRGB(x + column, y + row));
			}
			frames.add(copy);
		}
		return frames;
	}

	private static List<BufferedImage> splitFrames(BufferedImage image, FrameLayout layout) {
		if (layout.frameWidth() <= 0 || layout.frameHeight() <= 0 || layout.frameCount() <= 0) {
			throw new IllegalArgumentException("incorrect animation frame bounds: dimensions must be positive");
		}
		boolean vertical = image.getWidth() == layout.frameWidth()
				&& image.getHeight() == layout.frameHeight() * layout.frameCount();
		boolean horizontal = image.getHeight() == layout.frameHeight()
				&& image.getWidth() == layout.frameWidth() * layout.frameCount();
		if (!vertical && !horizontal) {
			throw new IllegalArgumentException("incorrect animation frame bounds: source does not exactly contain frames");
		}
		List<BufferedImage> frames = new ArrayList<>();
		for (int index = 0; index < layout.frameCount(); index++) {
			int x = horizontal && !vertical ? index * layout.frameWidth() : 0;
			int y = vertical ? index * layout.frameHeight() : 0;
			BufferedImage copy = new BufferedImage(layout.frameWidth(), layout.frameHeight(), BufferedImage.TYPE_INT_ARGB);
			for (int row = 0; row < layout.frameHeight(); row++) {
				for (int column = 0; column < layout.frameWidth(); column++) {
					copy.setRGB(column, row, image.getRGB(x + column, y + row));
				}
			}
			frames.add(copy);
		}
		return frames;
	}

	private static boolean touchesVisible(BufferedImage image, int x, int y) {
		for (int dy = -1; dy <= 1; dy++) {
			for (int dx = -1; dx <= 1; dx++) {
				if (dx == 0 && dy == 0) continue;
				int adjacentX = x + dx;
				int adjacentY = y + dy;
				if (adjacentX >= 0 && adjacentX < image.getWidth()
						&& adjacentY >= 0 && adjacentY < image.getHeight()
						&& (image.getRGB(adjacentX, adjacentY) >>> 24) != 0) return true;
			}
		}
		return false;
	}

	private static BufferedImage downsample(BufferedImage source) {
		int width = Math.max(1, source.getWidth() / 2);
		int height = Math.max(1, source.getHeight() / 2);
		BufferedImage target = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) target.setRGB(x, y, average(source, x * 2, y * 2));
		}
		return target;
	}

	private static int average(BufferedImage image, int originX, int originY) {
		long alpha = 0;
		long red = 0;
		long green = 0;
		long blue = 0;
		int samples = 0;
		for (int dy = 0; dy < 2; dy++) {
			for (int dx = 0; dx < 2; dx++) {
				int x = Math.min(originX + dx, image.getWidth() - 1);
				int y = Math.min(originY + dy, image.getHeight() - 1);
				int pixel = image.getRGB(x, y);
				int a = pixel >>> 24;
				alpha += a;
				red += (long) ((pixel >>> 16) & 255) * a;
				green += (long) ((pixel >>> 8) & 255) * a;
				blue += (long) (pixel & 255) * a;
				samples++;
			}
		}
		int outAlpha = (int) Math.round((double) alpha / samples);
		if (alpha == 0) return 0;
		int outRed = (int) Math.round((double) red / alpha);
		int outGreen = (int) Math.round((double) green / alpha);
		int outBlue = (int) Math.round((double) blue / alpha);
		return outAlpha << 24 | outRed << 16 | outGreen << 8 | outBlue;
	}

	public static void verifyOwnedPages(Path directory, Map<String, String> expected) {
		try {
			if (!Files.isDirectory(directory)) throw new IllegalStateException("Missing owned page directory: " + directory);
			List<String> actual;
			try (var stream = Files.list(directory)) {
				actual = stream.filter(Files::isRegularFile).map(path -> path.getFileName().toString())
						.sorted().toList();
			}
			List<String> expectedNames = expected.keySet().stream().sorted().toList();
			if (!actual.equals(expectedNames)) {
				throw new IllegalStateException("Owned page set mismatch; expected=" + expectedNames + ", actual=" + actual);
			}
			for (String name : expectedNames) {
				String actualDigest = sha256(directory.resolve(name));
				if (!actualDigest.equals(expected.get(name))) {
					throw new IllegalStateException("Owned page digest mismatch: " + name);
				}
			}
		} catch (IOException error) {
			throw new IllegalStateException("Could not verify owned pages", error);
		}
	}

	public static String sha256(Path file) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return HexFormat.of().formatHex(digest.digest(Files.readAllBytes(file)));
		} catch (Exception error) {
			throw new IllegalStateException("Could not hash " + file, error);
		}
	}

	public record FrameLayout(int frameWidth, int frameHeight, int frameCount) {
	}

	public record AnimationLayout(int frameWidth, int frameHeight, List<Integer> frameIndices,
			List<Integer> frameDurations) {
		public AnimationLayout {
			frameIndices = List.copyOf(frameIndices);
			frameDurations = List.copyOf(frameDurations);
			if (frameIndices.size() != frameDurations.size()) {
				throw new IllegalArgumentException("animation indices and durations differ");
			}
		}
	}

	public record ReviewedException(String path, String digest, String violation, String note) {
	}

	public record MipPixelResult(int frameIndex, int mipLevel, int width, int height,
			long transparentPixels, long translucentPixels, long opaquePixels) {
	}

	public record PixelEvidence(int width, int height, int frameCount, List<String> violations,
			List<MipPixelResult> mipResults) {
		public PixelEvidence(int width, int height, int frameCount, List<String> violations) {
			this(width, height, frameCount, violations, List.of());
		}

		public PixelEvidence {
			violations = List.copyOf(violations);
			mipResults = List.copyOf(mipResults);
		}

		public void requireReviewed(String path, String digest, List<ReviewedException> exceptions) {
			for (String violation : violations) {
				boolean reviewed = exceptions.stream().anyMatch(exception -> exception.path().equals(path)
						&& exception.digest().equals(digest)
						&& exception.violation().equals(violation)
						&& exception.note() != null && !exception.note().isBlank());
				if (!reviewed) throw new IllegalStateException("Unreviewed pixel violation: " + path + ": " + violation);
			}
		}
	}
}
