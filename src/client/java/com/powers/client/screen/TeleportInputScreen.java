package com.powers.client.screen;

import com.powers.PowersMod;
import com.powers.client.ClientPowerState;
import com.powers.network.PowersPackets;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
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
	private record DimEntry(String id, Component label) {
	}

	private static final Identifier PANEL = PowersMod.id("textures/gui/teleport_panel.png");
	private static final int PANEL_WIDTH = 256;
	private static final int PANEL_HEIGHT = 192;
	private static final List<DimEntry> DIMENSIONS = List.of(
			new DimEntry("minecraft:overworld", Component.translatable("dimension.minecraft.overworld")),
			new DimEntry("minecraft:the_nether", Component.translatable("dimension.minecraft.the_nether")),
			new DimEntry("minecraft:the_end", Component.translatable("dimension.minecraft.the_end")),
			new DimEntry("powers:dark_realm", Component.translatable("dimension.powers.dark_realm")),
			new DimEntry("powers:light_realm", Component.translatable("dimension.powers.light_realm")));

	private final int slot;
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
		super(Component.translatable("screen.powers.teleport"));
		this.slot = slot;
	}

	@Override
	protected void init() {
		int left = panelX();
		int top = panelY();
		available = ClientPowerState.canSeeDarkRealm()
				? DIMENSIONS
				: DIMENSIONS.stream().filter(entry -> !entry.id().equals("powers:dark_realm")).toList();
		List<Integer> dimensionValues = java.util.stream.IntStream.range(0, available.size()).boxed().toList();

		addRenderableWidget(CycleButton.<Integer>builder(this::modeName, () -> 0)
				.withValues(List.of(0, 1, 2)).displayOnlyValue()
				.create(left + 20, top + 32, 216, 20,
						Component.translatable("screen.powers.teleport.mode"), (button, value) -> {
							mode = value;
							updateModeWidgets();
						}));

		xField = coordinateField(left + 20, top + 61, "screen.powers.teleport.x", "X");
		yField = coordinateField(left + 132, top + 61, "screen.powers.teleport.y", "Y");
		zField = coordinateField(left + 20, top + 88, "screen.powers.teleport.z", "Z");
		dimensionButton = addRenderableWidget(CycleButton.<Integer>builder(
				index -> available.get(index).label(), () -> 0)
				.withValues(dimensionValues).displayOnlyValue()
				.create(left + 132, top + 88, 104, 20,
						Component.translatable("screen.powers.teleport.dimension"),
						(button, value) -> dimensionIndex = value));

		targetNameField = addRenderableWidget(new EditBox(font, left + 20, top + 116, 216, 20,
				Component.translatable("screen.powers.teleport.target")));
		targetNameField.setHint(Component.translatable("screen.powers.teleport.target_hint"));
		targetNameField.setMaxLength(16);
		addRenderableWidget(Button.builder(Component.translatable("screen.powers.teleport.go"),
				button -> confirm()).bounds(left + 68, top + 146, 120, 20).build());
		updateModeWidgets();
	}

	private EditBox coordinateField(int x, int y, String narrationKey, String hint) {
		EditBox field = addRenderableWidget(new EditBox(font, x, y, 104, 20,
				Component.translatable(narrationKey)));
		field.setHint(Component.literal(hint));
		field.setMaxLength(24);
		return field;
	}

	private Component modeName(int value) {
		return Component.translatable(switch (value) {
			case 0 -> "screen.powers.teleport.mode_self";
			case 1 -> "screen.powers.teleport.mode_other";
			default -> "screen.powers.teleport.mode_player";
		});
	}

	private void updateModeWidgets() {
		if (xField == null) return;
		boolean coordinates = mode != 2;
		xField.visible = coordinates;
		yField.visible = coordinates;
		zField.visible = coordinates;
		dimensionButton.visible = coordinates;
		targetNameField.visible = mode != 0;
		targetNameField.setY(panelY() + (mode == 2 ? 76 : 116));
	}

	private void confirm() {
		error = null;
		String target = targetNameField.getValue().trim();
		if (mode != 0 && target.isEmpty()) {
			error = Component.translatable("screen.powers.teleport.target_required");
			return;
		}
		try {
			if (mode == 2) {
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
				ClientPlayNetworking.send(new PowersPackets.TeleportRequestPayload(
						slot, x, y, z, key, mode == 1 ? target : "", false));
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
