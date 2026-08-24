package com.powers.client.acceptance;

import com.google.gson.Gson;
import com.powers.PowersMod;
import com.powers.PowersWeapons;
import com.powers.client.fx.FxAccessibility;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.item.TrackingItemStackRenderState;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.state.gui.GuiItemRenderState;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.PlayerModelType;
import org.joml.Matrix3x2f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/** Development gallery primitives; Minecraft's baked renderer remains the image authority. */
public final class VfxGalleryClientAgent {
	private static final Gson GSON = new Gson();
	private static final List<String> SPAWN_EGGS = List.of(
			"dark_herald_spawn_egg", "darkness_creature_spawn_egg", "first_vessel_spawn_egg",
			"light_herald_spawn_egg", "power_test_actor_spawn_egg", "radiant_sentinel_spawn_egg");
	private static final List<ItemDisplayContext> REQUIRED_CONTEXTS = List.of(
			ItemDisplayContext.GUI, ItemDisplayContext.GROUND,
			ItemDisplayContext.FIRST_PERSON_LEFT_HAND, ItemDisplayContext.FIRST_PERSON_RIGHT_HAND,
			ItemDisplayContext.THIRD_PERSON_LEFT_HAND, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
			ItemDisplayContext.FIXED, ItemDisplayContext.HEAD);
	private static final List<String> SHELF_ITEMS = List.of("amethyst_greatpick",
			"amethyst_greatshovel", "azure_pickaxe", "azure_shovel", "demons_blood_pick",
			"demons_blood_shovel", "talonshovel", "vaelith", "viridian_pickaxe", "viridian_shovel");
	private static final Pattern GUI_SCALE_CAPTURE = Pattern.compile("(?:^|/)scale([1-4])(?:/|$)");

	private VfxGalleryClientAgent() {
	}

	public static List<String> itemIds() {
		List<String> result = new ArrayList<>(PowersWeapons.allWeaponIds());
		result.addAll(SPAWN_EGGS);
		return List.copyOf(result);
	}

	public static List<String> spawnEggIds() {
		return SPAWN_EGGS;
	}

	public static List<ItemDisplayContext> requiredContexts() {
		return REQUIRED_CONTEXTS;
	}

	public static List<String> shelfItemIds() {
		return SHELF_ITEMS;
	}

	public static Set<String> requiredItemCaptureIds() {
		Set<String> result = new LinkedHashSet<>();
		for (String id : PowersWeapons.allWeaponIds()) for (ItemDisplayContext context : REQUIRED_CONTEXTS) {
			if (id.equals("runic_piercer") && context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND) continue;
			result.add("model/item/" + id + "/" + context.getSerializedName());
		}
		for (String id : SPAWN_EGGS) for (ItemDisplayContext context : REQUIRED_CONTEXTS) {
			result.add("item/" + id + "/" + context.getSerializedName());
		}
		for (String id : SHELF_ITEMS) result.add("model/item/" + id + "/on_shelf");
		return Set.copyOf(result);
	}

	public static String captureId(String itemId, ItemDisplayContext context) {
		return (SPAWN_EGGS.contains(itemId) ? "item/" : "model/item/") + itemId + "/"
				+ context.getSerializedName();
	}

	public static List<ItemStack> stacks(List<String> ids) {
		return ids.stream().map(id -> {
			var item = BuiltInRegistries.ITEM.getValue(PowersMod.id(id));
			if (item == null) throw new IllegalStateException("Missing gallery item powers:" + id);
			return item.getDefaultInstance();
		}).toList();
	}

	public static Screen itemScreen(String title, List<String> itemIds,
			ItemDisplayContext context, Background background) {
		return new ItemGalleryScreen(Component.literal(title), stacks(itemIds), context, background);
	}

	public static Screen entityScreen(String title, List<Integer> entityIds,
			EntityView view, Background background) {
		return new EntityGalleryScreen(Component.literal(title), entityIds, view, background);
	}

	public static List<UUID> wideAndSlimProfileIds() {
		UUID wide = null;
		UUID slim = null;
		for (long candidate = 1; candidate < 10_000 && (wide == null || slim == null); candidate++) {
			UUID id = new UUID(0x565846303131L, candidate);
			PlayerModelType type = DefaultPlayerSkin.get(id).model();
			if (type == PlayerModelType.WIDE && wide == null) wide = id;
			if (type == PlayerModelType.SLIM && slim == null) slim = id;
		}
		if (wide == null || slim == null) throw new IllegalStateException("Could not resolve deterministic wide/slim profiles");
		return List.of(wide, slim);
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	public static List<PlayerModelType> avatarModels(Minecraft client, List<Integer> entityIds) {
		List<PlayerModelType> result = new ArrayList<>();
		for (int index = 6; index < entityIds.size(); index++) {
			Entity entity = client.level == null ? null : client.level.getEntity(entityIds.get(index));
			if (entity == null) throw new IllegalStateException("Gallery avatar not synchronized: " + entityIds.get(index));
			EntityRenderer renderer = client.getEntityRenderDispatcher().getRenderer(entity);
			EntityRenderState state = renderer.createRenderState(entity, 1.0F);
			if (!(state instanceof AvatarRenderState avatar)) {
				throw new IllegalStateException("Shadow/Echo did not use AvatarRenderState: " + state.getClass());
			}
			result.add(avatar.skin.model());
		}
		return List.copyOf(result);
	}

	public static void applyOptions(Minecraft client, int mipLevel, boolean reducedMotion) {
		client.options.mipmapLevels().set(Math.clamp(mipLevel, 0, 4));
		client.options.particles().set(reducedMotion ? ParticleStatus.MINIMAL : ParticleStatus.ALL);
		client.options.screenEffectScale().set(reducedMotion ? 0.0 : 1.0);
	}

	public static void begin(Minecraft client) {
		Path output = metadataPath(client);
		try {
			Files.createDirectories(output.getParent());
			Files.writeString(output, "", StandardCharsets.UTF_8);
		} catch (IOException error) {
			throw new IllegalStateException("Could not initialise VFX gallery metadata", error);
		}
	}

	public static void assertRuntimeScaleMatchesCaptureIds(Minecraft client, List<String> captureIds) {
		int requested = client.options.guiScale().get();
		int effective = client.getWindow().getGuiScale();
		for (String captureId : captureIds) {
			var matcher = GUI_SCALE_CAPTURE.matcher(captureId);
			if (!matcher.find()) continue;
			int nominal = Integer.parseInt(matcher.group(1));
			if (requested != nominal || effective != nominal) {
				throw new AssertionError("Nominal GUI scale " + nominal
						+ " does not match requested/effective runtime scale "
						+ requested + "/" + effective + ": " + captureId);
			}
		}
	}

	public static void record(Minecraft client, String screenshot, List<String> captureIds,
			int mipLevel, boolean reducedMotion, Background background, String camera) {
		record(client, screenshot, captureIds, captureIds, mipLevel, reducedMotion, background, camera);
	}

	public static void record(Minecraft client, String screenshot, List<String> captureIds,
			List<String> sourceKeys, int mipLevel, boolean reducedMotion, Background background, String camera) {
		Path screenshotPath = client.gameDirectory.toPath().resolve("screenshots").resolve(screenshot);
		long gameTime = client.level == null ? -1L : client.level.getGameTime();
		String weather = client.level == null ? "unavailable"
				: client.level.isThundering() ? "thunder" : client.level.isRaining() ? "rain" : "clear";
		List<String> resourcePacks = client.getResourcePackRepository().getSelectedIds().stream().sorted().toList();
		RuntimeOptions options = new RuntimeOptions(client.getWindow().getWidth(), client.getWindow().getHeight(),
				client.options.guiScale().get(), (int) client.getWindow().getGuiScale(),
				client.options.mipmapLevels().get(), client.options.particles().get().toString(),
				client.options.screenEffectScale().get(), FxAccessibility.reducedMotion(client),
				client.options.renderDistance().get(), client.options.graphicsPreset().get().toString(),
				resourcePacks, gameTime, weather);
		CaptureMetadata metadata = new CaptureMetadata(screenshot, sha256(screenshotPath),
				List.copyOf(captureIds), List.copyOf(sourceKeys), options, background.serializedName, camera);
		try {
			Files.writeString(metadataPath(client), GSON.toJson(metadata) + "\n",
					StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
		} catch (IOException error) {
			throw new IllegalStateException("Could not record VFX gallery metadata", error);
		}
	}

	private static Path metadataPath(Minecraft client) {
		return client.gameDirectory.toPath().resolve("vfx-011-gallery/captures.jsonl");
	}

	private static String sha256(Path path) {
		try {
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path)));
		} catch (IOException | NoSuchAlgorithmException error) {
			throw new IllegalStateException("Could not hash VFX gallery screenshot " + path, error);
		}
	}

	public enum Background {
		LIGHT("light"), DARK("dark"), CHECKER("checker");

		private final String serializedName;

		Background(String serializedName) {
			this.serializedName = serializedName;
		}
	}

	public enum EntityView {
		FRONT(180.0F), BACK(0.0F), LEFT(90.0F), RIGHT(-90.0F), EQUIPPED(160.0F);

		private final float bodyRotation;

		EntityView(float bodyRotation) {
			this.bodyRotation = bodyRotation;
		}
	}

	public record CaptureMetadata(String screenshot, String screenshotSha256, List<String> captureIds,
			List<String> sourceKeys, RuntimeOptions runtimeOptions, String background, String camera) {
	}

	public record RuntimeOptions(int physicalWidth, int physicalHeight, int requestedGuiScale,
			int effectiveGuiScale, int mipLevel, String particles, double screenEffectScale,
			boolean reducedMotion, int renderDistance, String graphicsMode, List<String> resourcePacks,
			long gameTime, String weather) {
	}

	private static final class ItemGalleryScreen extends Screen {
		private final List<ItemStack> stacks;
		private final ItemDisplayContext context;
		private final Background background;

		private ItemGalleryScreen(Component title, List<ItemStack> stacks,
				ItemDisplayContext context, Background background) {
			super(title);
			this.stacks = List.copyOf(stacks);
			this.context = context;
			this.background = background;
		}

		@Override
		public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
			int dark = 0xFF101014;
			int light = 0xFFF4F0E8;
			if (background == Background.LIGHT || background == Background.DARK) {
				graphics.fill(0, 0, width, height, background == Background.LIGHT ? light : dark);
				return;
			}
			for (int y = 0; y < height; y += 32) {
				for (int x = 0; x < width; x += 32) {
					graphics.fill(x, y, Math.min(width, x + 32), Math.min(height, y + 32),
							((x / 32 + y / 32) & 1) == 0 ? light : dark);
				}
			}
		}

		@Override
		public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
			super.extractRenderState(graphics, mouseX, mouseY, delta);
			int columns = Math.max(1, Math.min(6, stacks.size()));
			int rows = Math.max(1, (stacks.size() + columns - 1) / columns);
			int cellWidth = Math.max(48, width / columns);
			int cellHeight = Math.max(40, (height - 40) / rows);
			int startX = (width - columns * cellWidth) / 2;
			int startY = 28;
			List<Label> labels = new ArrayList<>();
			for (int index = 0; index < stacks.size(); index++) {
				int x = startX + index % columns * cellWidth;
				int y = startY + index / columns * cellHeight;
				TrackingItemStackRenderState state = new TrackingItemStackRenderState();
				minecraft.getItemModelResolver().updateForTopItem(state, stacks.get(index), context,
						minecraft.level, minecraft.player, 31 * index + context.ordinal());
				var bounds = state.getModelBoundingBox();
				float maximumWidth = Math.max(24.0F, cellWidth - 14.0F);
				float maximumHeight = Math.max(24.0F, cellHeight - 24.0F);
				float modelWidth = Math.max(1.0F, (float) bounds.getXsize() * 16.0F);
				float modelHeight = Math.max(1.0F, (float) bounds.getYsize() * 16.0F);
				float scale = Math.min(3.0F, Math.min(maximumWidth / modelWidth, maximumHeight / modelHeight));
				float centreX = x + cellWidth / 2.0F;
				float centreY = y + maximumHeight / 2.0F;
				int itemX = Math.round(centreX - 8.0F
						- (float) (bounds.minX + bounds.maxX) * 8.0F);
				int itemY = Math.round(centreY - 8.0F
						+ (float) (bounds.minY + bounds.maxY) * 8.0F);
				Matrix3x2f pose = new Matrix3x2f(graphics.pose()).scaleAround(scale, centreX, centreY);
				graphics.guiRenderState.addItem(new GuiItemRenderState(pose, state, itemX, itemY, null));
				String label = BuiltInRegistries.ITEM.getKey(stacks.get(index).getItem()).getPath();
				labels.add(new Label(label, x, y + cellHeight - 18, cellWidth));
			}
			advancePastModelStrata(graphics);
			for (Label label : labels) {
				graphics.fill(label.x, label.y, label.x + label.width, label.y + 16, 0xE6101014);
				drawFittedLabel(graphics, label.text, label.x + label.width / 2,
						label.y + 4, label.width - 8, 0xFFF4F4F4);
			}
			graphics.centeredText(font, title, width / 2, 10,
					background == Background.LIGHT ? 0xFF181818 : 0xFFFFFFFF);
		}

		private void drawFittedLabel(GuiGraphicsExtractor graphics, String text, int centerX,
				int y, int availableWidth, int color) {
			float scale = Math.min(1.0F, availableWidth / (float) Math.max(1, font.width(text)));
			var pose = graphics.pose();
			pose.pushMatrix();
			pose.translate(centerX, y);
			pose.scale(scale, scale);
			graphics.centeredText(font, Component.literal(text), 0, 0, color);
			pose.popMatrix();
		}

		private static void advancePastModelStrata(GuiGraphicsExtractor graphics) {
			// Baked item layers can reserve several GUI strata; labels must remain an independent top band.
			for (int index = 0; index < 8; index++) graphics.nextStratum();
		}

		private record Label(String text, int x, int y, int width) {
		}
	}

	private static final class EntityGalleryScreen extends Screen {
		private final List<Integer> entityIds;
		private final EntityView view;
		private final Background background;

		private EntityGalleryScreen(Component title, List<Integer> entityIds,
				EntityView view, Background background) {
			super(title);
			this.entityIds = List.copyOf(entityIds);
			this.view = view;
			this.background = background;
		}

		@Override
		public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
			int first = background == Background.LIGHT ? 0xFFF4F0E8 : 0xFF101014;
			int second = background == Background.DARK ? 0xFF101014 : 0xFF777777;
			for (int y = 0; y < height; y += 48) for (int x = 0; x < width; x += 48) {
				graphics.fill(x, y, Math.min(width, x + 48), Math.min(height, y + 48),
						background == Background.CHECKER && ((x / 48 + y / 48) & 1) != 0 ? second : first);
			}
		}

		@Override
		@SuppressWarnings({"rawtypes", "unchecked"})
		public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
			super.extractRenderState(graphics, mouseX, mouseY, delta);
			int columns = 5;
			int cellWidth = Math.min(150, width / columns);
			int cellHeight = Math.min(250, (height - 28) / 2);
			int startX = (width - columns * cellWidth) / 2;
			List<Label> labels = new ArrayList<>();
			for (int index = 0; index < entityIds.size(); index++) {
				Entity entity = minecraft.level == null ? null : minecraft.level.getEntity(entityIds.get(index));
				if (entity == null) throw new IllegalStateException("Gallery entity not synchronized: " + entityIds.get(index));
				EntityRenderer renderer = minecraft.getEntityRenderDispatcher().getRenderer(entity);
				EntityRenderState state = renderer.createRenderState(entity, 1.0F);
				state.shadowPieces.clear();
				state.outlineColor = 0;
				if (state instanceof LivingEntityRenderState living) {
					living.bodyRot = view.bodyRotation;
					living.yRot = view == EntityView.EQUIPPED ? -20.0F : 0.0F;
					living.xRot = view == EntityView.EQUIPPED ? -8.0F : 0.0F;
					living.pose = Pose.STANDING;
					living.boundingBoxWidth /= living.scale;
					living.boundingBoxHeight /= living.scale;
					living.scale = 1.0F;
				}
				int column = index % columns;
				int row = index / columns;
				int x0 = startX + column * cellWidth + 4;
				int y0 = 26 + row * cellHeight;
				int x1 = x0 + cellWidth - 8;
				int y1 = y0 + cellHeight - 22;
				Quaternionf rotation = new Quaternionf().rotateZ((float) Math.PI);
				graphics.entity(state, Math.min(72, cellHeight / 2),
						new Vector3f(0.0F, state.boundingBoxHeight / 2.0F + 0.0625F, 0.0F),
						rotation, new Quaternionf(), x0, y0, x1, y1);
				String label = switch (index) {
					case 6 -> "shadow_wide_overlay";
					case 7 -> "shadow_slim_overlay";
					case 8 -> "echo_wide_overlay";
					case 9 -> "echo_slim_overlay";
					default -> BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath();
				};
				labels.add(new Label(label, x0 - 4, y1, cellWidth));
			}
			advancePastModelStrata(graphics);
			for (Label label : labels) {
				graphics.fill(label.x, label.y, label.x + label.width, label.y + 18, 0xE6101014);
				float scale = Math.min(1.0F,
						(label.width - 8) / (float) Math.max(1, font.width(label.text)));
				var pose = graphics.pose();
				pose.pushMatrix();
				pose.translate(label.x + label.width / 2, label.y + 5);
				pose.scale(scale, scale);
				graphics.centeredText(font, Component.literal(label.text), 0, 0, 0xFFF4F4F4);
				pose.popMatrix();
			}
			graphics.centeredText(font, title, width / 2, 10,
					background == Background.LIGHT ? 0xFF181818 : 0xFFFFFFFF);
		}

		private static void advancePastModelStrata(GuiGraphicsExtractor graphics) {
			// Equipped entity PIP states contain held-item layers in addition to the body renderer.
			for (int index = 0; index < 8; index++) graphics.nextStratum();
		}

		private record Label(String text, int x, int y, int width) {
		}
	}
}
