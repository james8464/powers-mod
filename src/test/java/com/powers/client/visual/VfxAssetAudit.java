package com.powers.client.visual;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Exact-identity structural inventory for every namespaced POWERS visual asset. */
public final class VfxAssetAudit {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
	private static final String PAGE_DIRECTORY = "docs/quality/vfx-011-asset-pages";
	private static final int PAGE_WIDTH = 1280;
	private static final int PAGE_HEIGHT = 1056;
	private static final int TILE_WIDTH = 80;
	private static final int TILE_HEIGHT = 88;
	private static final List<String> EGG_CONTEXTS = List.of("gui", "ground", "firstperson_lefthand",
			"firstperson_righthand", "thirdperson_lefthand", "thirdperson_righthand", "fixed", "head");

	private VfxAssetAudit() {
	}

	public static AuditManifest scan(Path root) {
		return generate(root).manifest();
	}

	private static AuditOutput generate(Path root) {
		try {
			Path assets = root.resolve("src/main/resources/assets/powers");
			if (!Files.isDirectory(assets)) throw new IllegalStateException("Missing assets/powers root: " + assets);
			List<Path> files;
			try (var walk = Files.walk(assets)) {
				files = walk.filter(Files::isRegularFile)
						.sorted(Comparator.comparing(path -> assets.relativize(path).toString())).toList();
			}
			Map<String, JsonObject> models = readModels(assets, files);
			validateModelParents(models);
			List<ReviewedRule> reviewed = readReviewedRules(root);
			List<MutableEntry> mutable = new ArrayList<>();
			for (Path file : files) mutable.add(inspect(assets, file, models, reviewed));
			ContactSheetOutput sheets = renderContactSheets(mutable);
			List<AssetEntry> entries = mutable.stream().map(MutableEntry::freeze).toList();
			Map<String, String> pageDigests = new LinkedHashMap<>();
			for (var page : sheets.pages().entrySet()) pageDigests.put(page.getKey(), sha256(page.getValue()));
			return new AuditOutput(new AuditManifest(2, List.copyOf(entries),
					Collections.unmodifiableMap(new LinkedHashMap<>(pageDigests)), sheets.pageTiles()), sheets.pages());
		} catch (IOException error) {
			throw new IllegalStateException("Could not scan VFX assets", error);
		}
	}

	public static void main(String[] arguments) throws Exception {
		if (arguments.length != 2 || !("--check".equals(arguments[1]) || "--update".equals(arguments[1]))) {
			throw new IllegalArgumentException("Usage: VfxAssetAudit <project-root> --check|--update");
		}
		Path root = Path.of(arguments[0]);
		Path output = root.resolve("docs/quality/vfx-011-asset-audit.json");
		AuditOutput audit = generate(root);
		String generated = GSON.toJson(audit.manifest()) + "\n";
		Path pages = root.resolve(PAGE_DIRECTORY);
		if ("--update".equals(arguments[1])) {
			Files.createDirectories(output.getParent());
			Files.writeString(output, generated, StandardCharsets.UTF_8);
			Files.createDirectories(pages);
			try (var existing = Files.list(pages)) {
				for (Path path : existing.filter(Files::isRegularFile).toList()) Files.delete(path);
			}
			for (var page : audit.pages().entrySet()) Files.write(pages.resolve(page.getKey()), page.getValue());
		} else if (!Files.isRegularFile(output)
				|| !generated.equals(Files.readString(output, StandardCharsets.UTF_8))) {
			throw new IllegalStateException("VFX asset audit manifest drift");
		} else {
			audit.manifest().requirePixelReviewComplete();
			VfxPixelAudit.verifyOwnedPages(pages, audit.manifest().pageDigests());
		}
	}

	private static Map<String, JsonObject> readModels(Path assets, List<Path> files) throws IOException {
		Map<String, JsonObject> result = new LinkedHashMap<>();
		for (Path file : files) {
			String relative = unix(assets.relativize(file));
			if (!relative.startsWith("models/") || !relative.endsWith(".json")) continue;
			JsonElement parsed = parseJson(file, relative);
			if (!parsed.isJsonObject()) throw invalid(relative, "model root must be an object");
			String key = "powers:" + relative.substring("models/".length(), relative.length() - 5);
			result.put(key, parsed.getAsJsonObject());
		}
		return result;
	}

	private static MutableEntry inspect(Path assets, Path file, Map<String, JsonObject> models,
			List<ReviewedRule> reviewed)
			throws IOException {
		String relative = unix(assets.relativize(file));
		String digest = sha256(file);
		String category = category(relative);
		int width = 0;
		int height = 0;
		int frames = 0;
		long transparent = 0;
		long translucent = 0;
		long opaque = 0;
		List<String> modelReferences = new ArrayList<>();
		List<String> textureReferences = new ArrayList<>();
		List<String> displayContexts = new ArrayList<>();
		List<String> liveCaptureIds = new ArrayList<>();
		int frameWidth = 0;
		int frameHeight = 0;
		int mipLevels = 0;
		List<Integer> frameIndices = List.of();
		List<Integer> frameDurations = List.of();
		List<VfxPixelAudit.MipPixelResult> mipResults = List.of();
		List<String> pixelViolations = List.of();
		String pixelReviewState = "NOT_A_PNG";
		List<List<BufferedImage>> mipChains = List.of();
		if (relative.endsWith(".png")) {
			BufferedImage image = ImageIO.read(file.toFile());
			if (image == null) throw invalid(relative, "PNG decode failed");
			width = image.getWidth();
			height = image.getHeight();
			AnimationSpec animation = animationSpec(file, image);
			frames = animation.indices().size();
			frameWidth = animation.frameWidth();
			frameHeight = animation.frameHeight();
			frameIndices = animation.indices();
			frameDurations = animation.durations();
			VfxPixelAudit.AnimationLayout layout = new VfxPixelAudit.AnimationLayout(frameWidth, frameHeight,
					frameIndices, frameDurations);
			VfxPixelAudit.PixelEvidence evidence = VfxPixelAudit.inspect(image, layout);
			mipChains = VfxPixelAudit.buildMipChains(image, layout);
			mipLevels = mipChains.stream().mapToInt(List::size).max().orElseThrow();
			mipResults = evidence.mipResults();
			pixelViolations = evidence.violations();
			boolean allReviewed = pixelViolations.stream().allMatch(violation -> reviewed.stream().anyMatch(rule ->
					rule.path().equals(relative) && rule.sha256().equals(digest)
							&& rule.violation().equals(violation) && !rule.note().isBlank()));
			pixelReviewState = pixelViolations.isEmpty() ? "NO_AUTOMATED_VIOLATIONS"
					: allReviewed ? "DIGEST_BOUND_REVIEW_EXCEPTION" : "NEEDS_REVIEW";
			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					int alpha = image.getRGB(x, y) >>> 24;
					if (alpha == 0) transparent++;
					else if (alpha == 255) opaque++;
					else translucent++;
				}
			}
		} else if (relative.endsWith(".json") || relative.endsWith(".mcmeta")) {
			JsonElement parsed = parseJson(file, relative);
			if (relative.startsWith("items/") && relative.endsWith(".json")) {
				collectItemModels(parsed, modelReferences);
				for (String reference : modelReferences) requireModel(relative, reference, models);
				if (relative.endsWith("_spawn_egg.json")) {
					String id = relative.substring("items/".length(), relative.length() - 5);
					for (String context : EGG_CONTEXTS) liveCaptureIds.add("item/" + id + "/" + context);
				}
			} else if (relative.startsWith("models/") && relative.endsWith(".json")) {
				JsonObject model = parsed.getAsJsonObject();
				validateModel(relative, model, models);
				collectModelReferences(model, modelReferences, textureReferences);
				JsonObject display = object(model, "display");
				if (display != null) {
					display.keySet().stream().sorted().forEach(displayContexts::add);
					String id = relative.substring("models/".length(), relative.length() - 5);
					for (String context : displayContexts) liveCaptureIds.add("model/" + id + "/" + context);
				}
			}
		}
		return new MutableEntry(relative, category, digest, width, height, frames,
				transparent, translucent, opaque, List.copyOf(modelReferences),
				List.copyOf(textureReferences), List.copyOf(displayContexts), new LinkedHashSet<>(),
				List.copyOf(liveCaptureIds), "PIXEL_EVIDENCE_ONLY", "Does not prove renderer output",
				frameWidth, frameHeight, frameIndices, frameDurations, mipLevels, mipResults,
				pixelViolations, pixelReviewState, mipChains);
	}

	private static void validateModel(String relative, JsonObject model, Map<String, JsonObject> models) {
		JsonArray elements = array(model, "elements");
		if (elements != null) {
			for (JsonElement value : elements) {
				if (!value.isJsonObject()) throw invalid(relative, "element must be an object");
				JsonObject element = value.getAsJsonObject();
				double[] from = triple(relative, element, "from", true);
				double[] to = triple(relative, element, "to", true);
				for (int axis = 0; axis < 3; axis++) {
					if (from[axis] > to[axis]) throw invalid(relative, "reversed element bounds");
				}
				JsonObject faces = object(element, "faces");
				if (faces != null) for (var face : faces.entrySet()) validateFace(relative, face.getValue());
			}
		}
		JsonObject display = object(model, "display");
		if (display != null) {
			for (var context : display.entrySet()) {
				if (!context.getValue().isJsonObject()) throw invalid(relative, "display context must be an object");
				JsonObject transform = context.getValue().getAsJsonObject();
				for (String key : List.of("rotation", "translation", "scale")) {
					if (transform.has(key)) triple(relative, transform, key, false);
				}
			}
		}
		Map<String, String> textures = mergedTextures(relative, model, models, new LinkedHashSet<>());
		for (String key : textures.keySet()) resolveVariable(relative, key, textures, new ArrayDeque<>());
		validateTextureUses(relative, model, textures);
	}

	private static void validateFace(String relative, JsonElement value) {
		if (!value.isJsonObject()) throw invalid(relative, "face must be an object");
		JsonObject face = value.getAsJsonObject();
		if (!face.has("uv")) return;
		JsonArray uv = face.getAsJsonArray("uv");
		if (uv.size() != 4) throw invalid(relative, "UV must contain four values");
		double[] values = new double[4];
		for (int index = 0; index < 4; index++) {
			values[index] = finite(relative, uv.get(index), "UV");
			if (values[index] < 0.0 || values[index] > 16.0) throw invalid(relative, "UV outside [0,16]");
		}
		if (values[0] == values[2] || values[1] == values[3]) throw invalid(relative, "zero-area UV");
	}

	private static double[] triple(String relative, JsonObject object, String key, boolean required) {
		if (!object.has(key)) {
			if (required) throw invalid(relative, "missing " + key + " display triple");
			return new double[3];
		}
		JsonElement value = object.get(key);
		if (!value.isJsonArray() || value.getAsJsonArray().size() != 3) {
			throw invalid(relative, key + " display triple must contain three values");
		}
		double[] result = new double[3];
		for (int index = 0; index < 3; index++) {
			result[index] = finite(relative, value.getAsJsonArray().get(index), "display value");
		}
		return result;
	}

	private static double finite(String relative, JsonElement value, String label) {
		if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
			throw invalid(relative, label + " must be numeric");
		}
		double result = value.getAsDouble();
		if (!Double.isFinite(result)) throw invalid(relative, "non-finite " + label);
		return result;
	}

	private static void validateModelParents(Map<String, JsonObject> models) {
		for (var entry : models.entrySet()) mergedTextures(entry.getKey(), entry.getValue(), models,
				new LinkedHashSet<>());
	}

	private static Map<String, String> mergedTextures(String relative, JsonObject model,
			Map<String, JsonObject> models, LinkedHashSet<String> visiting) {
		String identity = relative.startsWith("powers:") ? relative : "powers:" + relative;
		if (!visiting.add(identity)) throw invalid(relative, "cyclic model parent reference");
		Map<String, String> result = new LinkedHashMap<>();
		if (model.has("parent")) {
			String parent = model.get("parent").getAsString();
			if (parent.startsWith("powers:")) {
				JsonObject inherited = models.get(parent);
				if (inherited == null) throw invalid(relative, "unknown model reference " + parent);
				result.putAll(mergedTextures(parent, inherited, models, visiting));
			}
		}
		JsonObject textures = object(model, "textures");
		if (textures != null) for (var texture : textures.entrySet()) {
			if (!texture.getValue().isJsonPrimitive()) throw invalid(relative, "texture value must be a string");
			result.put(texture.getKey(), texture.getValue().getAsString());
		}
		visiting.remove(identity);
		return result;
	}

	private static String resolveVariable(String relative, String key, Map<String, String> textures,
			ArrayDeque<String> stack) {
		if (!textures.containsKey(key)) throw invalid(relative, "unresolved texture variable #" + key);
		if (stack.contains(key)) throw invalid(relative, "cyclic texture variable " + stack + " -> " + key);
		String value = textures.get(key);
		if (!value.startsWith("#")) return value;
		stack.addLast(key);
		String result = resolveVariable(relative, value.substring(1), textures, stack);
		stack.removeLast();
		return result;
	}

	private static void validateTextureUses(String relative, JsonObject model, Map<String, String> textures) {
		JsonArray elements = array(model, "elements");
		if (elements == null) return;
		for (JsonElement elementValue : elements) {
			JsonObject faces = object(elementValue.getAsJsonObject(), "faces");
			if (faces == null) continue;
			for (var face : faces.entrySet()) {
				JsonObject value = face.getValue().getAsJsonObject();
				if (value.has("texture") && value.get("texture").getAsString().startsWith("#")) {
					resolveVariable(relative, value.get("texture").getAsString().substring(1), textures,
							new ArrayDeque<>());
				}
			}
		}
	}

	private static void collectItemModels(JsonElement value, List<String> target) {
		if (value.isJsonObject()) {
			for (var entry : value.getAsJsonObject().entrySet()) {
				if ("model".equals(entry.getKey()) && entry.getValue().isJsonPrimitive()) {
					String reference = entry.getValue().getAsString();
					if (reference.startsWith("powers:")) target.add(reference);
				} else collectItemModels(entry.getValue(), target);
			}
		} else if (value.isJsonArray()) for (JsonElement child : value.getAsJsonArray()) {
			collectItemModels(child, target);
		}
	}

	private static void collectModelReferences(JsonObject model, List<String> models, List<String> textures) {
		if (model.has("parent") && model.get("parent").getAsString().startsWith("powers:")) {
			models.add(model.get("parent").getAsString());
		}
		JsonObject textureMap = object(model, "textures");
		if (textureMap != null) for (JsonElement value : textureMap.asMap().values()) {
			String reference = value.getAsString();
			if (reference.startsWith("powers:")) textures.add(reference);
		}
	}

	private static void requireModel(String relative, String reference, Map<String, JsonObject> models) {
		if (!models.containsKey(reference)) throw invalid(relative, "unknown model reference " + reference);
	}

	private static JsonElement parseJson(Path file, String relative) throws IOException {
		try {
			return JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8));
		} catch (RuntimeException error) {
			throw invalid(relative, "malformed JSON: " + error.getMessage());
		}
	}

	private static AnimationSpec animationSpec(Path file, BufferedImage image) throws IOException {
		Path metadata = file.resolveSibling(file.getFileName() + ".mcmeta");
		if (!Files.isRegularFile(metadata)) {
			return new AnimationSpec(image.getWidth(), image.getHeight(), List.of(0), List.of(1));
		}
		JsonObject root = parseJson(metadata, metadata.getFileName().toString()).getAsJsonObject();
		JsonObject animation = object(root, "animation");
		if (animation == null) throw invalid(file.toString(), "animation metadata lacks animation object");
		int frameWidth = animation.has("width") ? animation.get("width").getAsInt() : image.getWidth();
		int frameHeight = animation.has("height") ? animation.get("height").getAsInt() : frameWidth;
		if (frameWidth <= 0 || frameHeight <= 0 || image.getWidth() % frameWidth != 0
				|| image.getHeight() % frameHeight != 0) {
			throw invalid(file.toString(), "incorrect animation frame bounds");
		}
		int physicalFrames = image.getWidth() / frameWidth * (image.getHeight() / frameHeight);
		int defaultDuration = animation.has("frametime") ? animation.get("frametime").getAsInt() : 1;
		if (defaultDuration <= 0) throw invalid(file.toString(), "animation frame duration must be positive");
		List<Integer> indices = new ArrayList<>();
		List<Integer> durations = new ArrayList<>();
		if (animation.has("frames")) {
			JsonArray timeline = animation.getAsJsonArray("frames");
			if (timeline.isEmpty()) throw invalid(file.toString(), "animation timeline must not be empty");
			for (JsonElement value : timeline) {
				int index;
				int duration = defaultDuration;
				if (value.isJsonObject()) {
					JsonObject frame = value.getAsJsonObject();
					if (!frame.has("index")) throw invalid(file.toString(), "animation frame lacks index");
					index = frame.get("index").getAsInt();
					if (frame.has("time")) duration = frame.get("time").getAsInt();
				} else index = value.getAsInt();
				if (index < 0 || index >= physicalFrames) throw invalid(file.toString(),
						"animation frame index outside source: " + index);
				if (duration <= 0) throw invalid(file.toString(), "animation frame duration must be positive");
				indices.add(index);
				durations.add(duration);
			}
		} else for (int index = 0; index < physicalFrames; index++) {
			indices.add(index);
			durations.add(defaultDuration);
		}
		return new AnimationSpec(frameWidth, frameHeight, List.copyOf(indices), List.copyOf(durations));
	}

	private static List<ReviewedRule> readReviewedRules(Path root) throws IOException {
		Path file = root.resolve("docs/quality/vfx-011-reviewed-exceptions.json");
		if (!Files.isRegularFile(file)) return List.of();
		JsonObject object = parseJson(file, unix(root.relativize(file))).getAsJsonObject();
		JsonArray exceptions = array(object, "exceptions");
		if (exceptions == null) return List.of();
		List<ReviewedRule> result = new ArrayList<>();
		for (JsonElement value : exceptions) {
			JsonObject rule = value.getAsJsonObject();
			result.add(new ReviewedRule(rule.get("path").getAsString(), rule.get("sha256").getAsString(),
					rule.get("violation").getAsString(), rule.get("note").getAsString()));
		}
		return List.copyOf(result);
	}

	private static ContactSheetOutput renderContactSheets(List<MutableEntry> entries) throws IOException {
		List<Tile> tiles = new ArrayList<>();
		for (MutableEntry entry : entries) {
			for (int frame = 0; frame < entry.mipChains().size(); frame++) {
				List<BufferedImage> chain = entry.mipChains().get(frame);
				for (int mip = 0; mip < chain.size(); mip++) tiles.add(new Tile(entry, frame, mip, chain.get(mip)));
			}
		}
		int columns = PAGE_WIDTH / TILE_WIDTH;
		int rows = PAGE_HEIGHT / TILE_HEIGHT;
		int perPage = columns * rows;
		Map<String, byte[]> pages = new LinkedHashMap<>();
		List<PageTile> pageTiles = new ArrayList<>();
		for (Background background : Background.values()) {
			for (int offset = 0, pageIndex = 0; offset < tiles.size(); offset += perPage, pageIndex++) {
				String pageName = String.format("pixels-%s-%03d.png", background.id, pageIndex);
				BufferedImage page = new BufferedImage(PAGE_WIDTH, PAGE_HEIGHT, BufferedImage.TYPE_INT_ARGB);
				Graphics2D graphics = page.createGraphics();
				graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
				paintBackground(graphics, background);
				int limit = Math.min(tiles.size(), offset + perPage);
				for (int index = offset; index < limit; index++) {
					Tile tile = tiles.get(index);
					int slot = index - offset;
					int x = slot % columns * TILE_WIDTH;
					int y = slot / columns * TILE_HEIGHT;
					graphics.setColor(new Color(255, 255, 255, 96));
					graphics.drawRect(x, y, TILE_WIDTH - 1, TILE_HEIGHT - 1);
					int scale = Math.max(1, Math.min(64 / tile.image().getWidth(), 64 / tile.image().getHeight()));
					int drawWidth = Math.max(1, tile.image().getWidth() * scale);
					int drawHeight = Math.max(1, tile.image().getHeight() * scale);
					if (drawWidth > 64 || drawHeight > 64) {
						double fit = Math.min(64.0 / tile.image().getWidth(), 64.0 / tile.image().getHeight());
						drawWidth = Math.max(1, (int) Math.floor(tile.image().getWidth() * fit));
						drawHeight = Math.max(1, (int) Math.floor(tile.image().getHeight() * fit));
					}
					graphics.drawImage(tile.image(), x + (TILE_WIDTH - drawWidth) / 2,
							y + 4 + (64 - drawHeight) / 2, drawWidth, drawHeight, null);
					paintNumber(graphics, x + 3, y + 72, index, Color.WHITE);
					tile.entry().sheetPageIds().add(pageName);
					pageTiles.add(new PageTile(pageName, index, slot, tile.entry().path(), tile.frame(), tile.mip(),
							x, y, TILE_WIDTH, TILE_HEIGHT, background.id));
				}
				graphics.dispose();
				ByteArrayOutputStream bytes = new ByteArrayOutputStream();
				ImageIO.write(page, "png", bytes);
				pages.put(pageName, bytes.toByteArray());
			}
		}
		return new ContactSheetOutput(Collections.unmodifiableMap(new LinkedHashMap<>(pages)), List.copyOf(pageTiles));
	}

	private static void paintBackground(Graphics2D graphics, Background background) {
		if (background == Background.CHECKER) {
			for (int y = 0; y < PAGE_HEIGHT; y += 16) for (int x = 0; x < PAGE_WIDTH; x += 16) {
				graphics.setColor(((x + y) / 16 & 1) == 0 ? new Color(54, 54, 54) : new Color(202, 202, 202));
				graphics.fillRect(x, y, 16, 16);
			}
		} else {
			graphics.setColor(background == Background.LIGHT ? new Color(238, 238, 238) : new Color(20, 20, 24));
			graphics.fillRect(0, 0, PAGE_WIDTH, PAGE_HEIGHT);
		}
	}

	private static final int[][] DIGITS = {
		{7,5,5,5,7},{2,6,2,2,7},{7,1,7,4,7},{7,1,7,1,7},{5,5,7,1,1},
		{7,4,7,1,7},{7,4,7,5,7},{7,1,1,1,1},{7,5,7,5,7},{7,5,7,1,7}
	};

	private static void paintNumber(Graphics2D graphics, int x, int y, int value, Color color) {
		graphics.setColor(color);
		String text = Integer.toString(value);
		for (int digit = 0; digit < text.length(); digit++) {
			int[] glyph = DIGITS[text.charAt(digit) - '0'];
			for (int row = 0; row < 5; row++) for (int column = 0; column < 3; column++) {
				if ((glyph[row] & 1 << (2 - column)) != 0) graphics.fillRect(x + digit * 4 + column, y + row, 1, 1);
			}
		}
	}

	private static JsonObject object(JsonObject parent, String key) {
		if (!parent.has(key)) return null;
		if (!parent.get(key).isJsonObject()) throw invalid(key, key + " must be an object");
		return parent.getAsJsonObject(key);
	}

	private static JsonArray array(JsonObject parent, String key) {
		if (!parent.has(key)) return null;
		if (!parent.get(key).isJsonArray()) throw invalid(key, key + " must be an array");
		return parent.getAsJsonArray(key);
	}

	private static String category(String relative) {
		if (relative.startsWith("items/")) return "item_definition";
		if (relative.startsWith("models/item/")) return "item_model";
		if (relative.startsWith("models/block/")) return "block_model";
		if (relative.startsWith("textures/item/") || relative.startsWith("textures/imported/")) return "item_texture";
		if (relative.startsWith("textures/block/")) return "block_texture";
		if (relative.startsWith("textures/entity/")) return "entity_skin";
		if (relative.startsWith("textures/gui/")) return "gui_texture";
		if (relative.startsWith("textures/particle/")) return "particle_texture";
		if (relative.endsWith(".ogg")) return "sound";
		return relative.contains("/") ? relative.substring(0, relative.indexOf('/')) : "namespace_root";
	}

	private static IllegalStateException invalid(String relative, String message) {
		return new IllegalStateException(relative + ": " + message);
	}

	private static String sha256(Path path) throws IOException {
		return sha256(Files.readAllBytes(path));
	}

	private static String sha256(byte[] bytes) {
		try {
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
		} catch (java.security.GeneralSecurityException error) {
			throw new IllegalStateException("SHA-256 unavailable", error);
		}
	}

	private static String unix(Path path) {
		return path.toString().replace('\\', '/');
	}

	public record AuditManifest(int schema, List<AssetEntry> assets, Map<String, String> pageDigests,
			List<PageTile> pageTiles) {
		public AuditManifest {
			assets = List.copyOf(assets);
			pageDigests = Collections.unmodifiableMap(new LinkedHashMap<>(pageDigests));
			pageTiles = List.copyOf(pageTiles);
		}

		public void requireLiveCaptures(Set<String> emitted) {
			Set<String> actual = Set.copyOf(emitted);
			for (AssetEntry asset : assets) for (String required : asset.liveCaptureIds()) {
				if (!actual.contains(required)) {
					String context = required.substring(required.lastIndexOf('/') + 1);
					if (asset.path().contains("spawn_egg")) {
						throw new IllegalStateException("missing required spawn-egg display context " + context);
					}
					throw new IllegalStateException("missing required live capture " + required);
				}
			}
		}

		public void requirePixelReviewComplete() {
			List<String> pending = assets.stream().filter(asset -> asset.path().endsWith(".png"))
					.filter(asset -> "NEEDS_REVIEW".equals(asset.pixelReviewState()))
					.map(AssetEntry::path).toList();
			if (!pending.isEmpty()) throw new IllegalStateException("Unreviewed pixel violations: " + pending);
		}
	}

	public record AssetEntry(String path, String category, String sha256, int width, int height,
			int frameCount, long transparentPixels, long translucentPixels, long opaquePixels,
			List<String> modelReferences, List<String> textureReferences, List<String> displayContexts,
			List<String> sheetPageIds, List<String> liveCaptureIds, String verdict, String notes,
			int frameWidth, int frameHeight, List<Integer> frameIndices, List<Integer> frameDurations,
			int mipLevels, List<VfxPixelAudit.MipPixelResult> mipResults, List<String> pixelViolations,
			String pixelReviewState) {
		public AssetEntry {
			modelReferences = List.copyOf(modelReferences);
			textureReferences = List.copyOf(textureReferences);
			displayContexts = List.copyOf(displayContexts);
			sheetPageIds = List.copyOf(sheetPageIds);
			liveCaptureIds = List.copyOf(liveCaptureIds);
			frameIndices = List.copyOf(frameIndices);
			frameDurations = List.copyOf(frameDurations);
			mipResults = List.copyOf(mipResults);
			pixelViolations = List.copyOf(pixelViolations);
		}
	}

	private record MutableEntry(String path, String category, String sha256, int width, int height,
			int frameCount, long transparentPixels, long translucentPixels, long opaquePixels,
			List<String> modelReferences, List<String> textureReferences, List<String> displayContexts,
			LinkedHashSet<String> sheetPageIds, List<String> liveCaptureIds, String verdict, String notes,
			int frameWidth, int frameHeight, List<Integer> frameIndices, List<Integer> frameDurations,
			int mipLevels, List<VfxPixelAudit.MipPixelResult> mipResults, List<String> pixelViolations,
			String pixelReviewState, List<List<BufferedImage>> mipChains) {
		AssetEntry freeze() {
			return new AssetEntry(path, category, sha256, width, height, frameCount, transparentPixels,
					translucentPixels, opaquePixels, modelReferences, textureReferences, displayContexts,
					List.copyOf(sheetPageIds), liveCaptureIds, verdict, notes, frameWidth, frameHeight,
					frameIndices, frameDurations, mipLevels, mipResults, pixelViolations, pixelReviewState);
		}
	}

	private record AuditOutput(AuditManifest manifest, Map<String, byte[]> pages) {
	}

	private record ContactSheetOutput(Map<String, byte[]> pages, List<PageTile> pageTiles) {
	}

	public record PageTile(String page, int tileId, int slot, String path, int physicalFrame, int mipLevel,
			int x, int y, int width, int height, String background) {
	}

	private record Tile(MutableEntry entry, int frame, int mip, BufferedImage image) {
	}

	private record AnimationSpec(int frameWidth, int frameHeight, List<Integer> indices,
			List<Integer> durations) {
	}

	private record ReviewedRule(String path, String sha256, String violation, String note) {
	}

	private enum Background {
		LIGHT("light"), DARK("dark"), CHECKER("checker");
		private final String id;
		Background(String id) { this.id = id; }
	}
}
