package com.powers.client.screen;

import com.powers.PowersMod;
import com.powers.client.ClientPowerState;
import com.powers.network.PowersPackets;
import com.powers.network.ShadowSwordPackets;
import com.powers.power.travel.TeleportDimensionMenu;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.List;

/** Responsive, texture-backed destination ritual for time-shift travel. */
public final class TeleportInputScreen extends Screen {
	public enum OwnerSurface { INNATE, ARTIFACT }

	private record DimEntry(String id, Component label) {
	}

	private static final Identifier PANEL = PowersMod.id("textures/gui/teleport_panel.png");
	private static final int PANEL_WIDTH = 256;
	private static final int PANEL_HEIGHT = 192;
	private static final List<String> FALLBACK_DIMENSIONS = List.of(
			"minecraft:overworld", "minecraft:the_nether", "minecraft:the_end",
			"powers:dark_realm", "powers:light_realm");

	private final int slot;
	private final String artifactAlignment;
	private final long actionRevision;
	private final String artifactActionKey;
	private final OwnerSurface ownerSurface;
	private List<DimEntry> available = List.of();
	private EditBox xField;
	private EditBox yField;
	private EditBox zField;
	private EditBox targetNameField;
	private CycleButton<Integer> dimensionButton;
	private int dimensionIndex;
	private int mode;
	private Component error;

	public TeleportInputScreen(int slot) {
		this(slot, null, 0L, "", OwnerSurface.INNATE);
	}

	private TeleportInputScreen(int slot, String artifactAlignment, long actionRevision,
			String artifactActionKey, OwnerSurface ownerSurface) {
		super(Component.translatable("screen.powers.teleport"));
		this.slot = slot;
		this.artifactAlignment = artifactAlignment;
		this.actionRevision = actionRevision;
		this.artifactActionKey = artifactActionKey;
		this.ownerSurface = ownerSurface;
	}

	public static TeleportInputScreen shadowSword() {
		return artifact("darkness");
	}

	public static TeleportInputScreen artifact(String alignment) {
		return artifact(com.powers.client.ClientActionRegistry.revision(), alignment,
				com.powers.client.ClientActionRegistry.artifactActionKey());
	}

	public static TeleportInputScreen artifact(long revision, String alignment, String actionKey) {
		return new TeleportInputScreen(-1, alignment, revision, actionKey, OwnerSurface.ARTIFACT);
	}

	public OwnerSurface ownerSurface() {
		return ownerSurface;
	}

	/** Drives the production validation state during renderer acceptance. */
	public boolean submitAcceptanceCoordinates(String x, String y, String z) {
		if (xField == null || yField == null || zField == null) return false;
		xField.setValue(x);
		yField.setValue(y);
		zField.setValue(z);
		confirm();
		return error != null;
	}

	@Override
	protected void init() {
		int left = panelX();
		int top = panelY();
		available = TeleportDimensionMenu.visibleIds(serverDimensionIds(), ClientPowerState.canSeeDarkRealm())
				.stream().map(this::dimensionEntry).toList();
		List<Integer> dimensionValues = java.util.stream.IntStream.range(0, available.size()).boxed().toList();

		addRenderableWidget(CycleButton.<Integer>builder(this::modeName, () -> 0)
				.withValues(artifactAlignment == null ? List.of(0, 1) : List.of(0)).displayOnlyValue()
				.create(left + 20, top + 32, 216, 20,
						Component.translatable("screen.powers.teleport.mode"), (button, value) -> {
							mode = value;
							updateModeWidgets();
						}));

		xField = coordinateField(left + 20, top + 61, "screen.powers.teleport.x", "X", 68);
		yField = coordinateField(left + 94, top + 61, "screen.powers.teleport.y", "Y", 68);
		zField = coordinateField(left + 168, top + 61, "screen.powers.teleport.z", "Z", 68);
		dimensionButton = addRenderableWidget(CycleButton.<Integer>builder(
				index -> available.get(index).label(), () -> 0)
				.withValues(dimensionValues).displayOnlyValue()
				.create(left + 20, top + 88, 216, 20,
						Component.translatable("screen.powers.teleport.dimension"),
						(button, value) -> {
							dimensionIndex = value;
							button.setTooltip(Tooltip.create(Component.literal(available.get(value).id())));
						}));
		dimensionButton.setTooltip(Tooltip.create(Component.literal(available.getFirst().id())));

		targetNameField = addRenderableWidget(new EditBox(font, left + 20, top + 116, 216, 20,
				Component.translatable("screen.powers.teleport.target")));
		targetNameField.setHint(Component.translatable("screen.powers.teleport.target_hint"));
		targetNameField.setMaxLength(64);
		addRenderableWidget(Button.builder(Component.translatable("screen.powers.teleport.go"),
				button -> confirm()).bounds(left + 68, top + 146, 120, 20).build());
		updateModeWidgets();
	}

	private EditBox coordinateField(int x, int y, String narrationKey, String hint, int width) {
		EditBox field = addRenderableWidget(new EditBox(font, x, y, width, 20,
				Component.translatable(narrationKey)));
		field.setHint(Component.literal(hint));
		field.setMaxLength(24);
		return field;
	}

	private List<String> serverDimensionIds() {
		var connection = Minecraft.getInstance().getConnection();
		return connection == null || connection.levels().isEmpty() ? FALLBACK_DIMENSIONS
				: connection.levels().stream().map(key -> key.identifier().toString()).toList();
	}

	private DimEntry dimensionEntry(String id) {
		Identifier parsed = Identifier.tryParse(id);
		if (parsed == null) return new DimEntry(id, Component.literal(id));
		String key = "dimension." + parsed.getNamespace() + "." + parsed.getPath();
		return new DimEntry(id, Component.translatableWithFallback(key, id));
	}

	private Component modeName(int value) {
		return Component.translatable(value == 0
				? "screen.powers.teleport.mode_self" : "screen.powers.teleport.mode_player");
	}

	private void updateModeWidgets() {
		if (xField == null) return;
		boolean coordinates = mode == 0;
		xField.visible = coordinates;
		yField.visible = coordinates;
		zField.visible = coordinates;
		dimensionButton.visible = coordinates;
		targetNameField.visible = mode == 1;
		targetNameField.setY(panelY() + 76);
	}

	private void confirm() {
		error = null;
		String target = targetNameField.getValue().trim();
		if (mode != 0 && target.isEmpty()) {
			error = Component.translatable("screen.powers.teleport.target_required");
			return;
		}
		try {
			if (mode == 1) {
				ClientPowerState.markingSlot = slot;
				ClientPowerState.markingTicks = 200;
				ClientPlayNetworking.send(new PowersPackets.TeleportRequestPayload(
						slot, 0, 0, 0,
						ResourceKey.create(Registries.DIMENSION,
								Identifier.fromNamespaceAndPath("minecraft", "overworld")), target, true));
			} else {
				double x = Double.parseDouble(xField.getValue().trim());
				double y = Double.parseDouble(yField.getValue().trim());
				double z = Double.parseDouble(zField.getValue().trim());
				if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
					throw new NumberFormatException("Coordinates must be finite");
				}
				Identifier id = Identifier.tryParse(available.get(dimensionIndex).id());
				if (id == null) throw new IllegalStateException("Registered dimension ID became invalid");
				ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, id);
				if (artifactAlignment != null) {
					ClientPlayNetworking.send(new ShadowSwordPackets.TeleportPayload(
							actionRevision, artifactAlignment, artifactActionKey,
							x, y, z, key, mode == 1 ? target : ""));
				} else {
					ClientPlayNetworking.send(new PowersPackets.TeleportRequestPayload(
							slot, x, y, z, key, mode == 1 ? target : "", false));
				}
			}
			onClose();
		} catch (NumberFormatException exception) {
			error = Component.translatable("screen.powers.teleport.invalid");
		}
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractBackground(graphics, mouseX, mouseY, delta);
		graphics.blit(RenderPipelines.GUI_TEXTURED, PANEL, panelX(), panelY(), 0, 0,
				PANEL_WIDTH, PANEL_HEIGHT, PANEL_WIDTH, PANEL_HEIGHT);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);
		graphics.centeredText(font, title, width / 2, panelY() + 14, 0xFFEAFBFF);
		if (error != null) graphics.centeredText(font, error, width / 2, panelY() + 174, 0xFFFF7777);
	}

	private int panelX() {
		return (width - PANEL_WIDTH) / 2;
	}

	private int panelY() {
		return Math.max(8, (height - PANEL_HEIGHT) / 2);
	}
}
