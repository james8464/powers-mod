package com.powers.client.screen;

import com.powers.client.ClientPowerState;
import com.powers.network.PowersPackets;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.List;

/** the input screen for the time shift power: type coordinates or a player name and pick a dimension */
public class TeleportInputScreen extends Screen {
	private record DimEntry(String id, String label) {}

	private static final List<DimEntry> DIMENSIONS = List.of(
			new DimEntry("minecraft:overworld", "The Overworld"),
			new DimEntry("minecraft:the_nether", "The Nether"),
			new DimEntry("minecraft:the_end", "The End"),
			new DimEntry("powers:dark_realm", "Dark Realm"),
			new DimEntry("powers:light_realm", "Light Realm"));

	private final int slot;

	// the destinations on offer this session; the dark realm is hidden from
	// players who haven't earned the darkness mark at rank 5+
	private List<DimEntry> available;

	private EditBox xField, yField, zField, targetNameField;
	private int dimIndex;
	private int mode;
	private Component error;

	public TeleportInputScreen(int slot) {
		super(Component.translatable("screen.powers.teleport"));
		this.slot = slot;
	}

	@Override
	protected void init() {
		int cx = this.width / 2;
		int fw = 120;

		available = ClientPowerState.canSeeDarkRealm()
				? DIMENSIONS
				: DIMENSIONS.stream().filter(e -> !e.id().equals("powers:dark_realm")).toList();
		List<String> dimNames = available.stream().map(e -> e.label()).toList();
		List<Integer> dimValues = new java.util.ArrayList<>();
		for (int i = 0; i < available.size(); i++) dimValues.add(i);
		this.addRenderableWidget(CycleButton.<Integer>builder(
						idx -> switch (idx) {
							case 0 -> Component.literal("Self (coords)");
							case 1 -> Component.literal("Other (coords)");
							default -> Component.literal("To Player");
						}, () -> 0)
				.withValues(List.of(0, 1, 2))
				.displayOnlyValue()
				.create(cx - 88, 36, 176, 20, Component.literal("Mode"),
						(btn, val) -> mode = val));

		this.xField = addRenderableWidget(new EditBox(this.font, cx - fw - 8, 62, fw, 20,
				Component.translatable("screen.powers.teleport.x")));
		this.yField = addRenderableWidget(new EditBox(this.font, cx + 4, 62, fw, 20,
				Component.translatable("screen.powers.teleport.y")));
		this.zField = addRenderableWidget(new EditBox(this.font, cx - fw - 8, 86, fw, 20,
				Component.translatable("screen.powers.teleport.z")));

		this.addRenderableWidget(CycleButton.<Integer>builder(
						idx -> Component.literal(dimNames.get(idx)), () -> 0)
				.withValues(dimValues)
				.displayOnlyValue()
				.create(cx + 4, 86, fw, 20, Component.translatable("screen.powers.teleport.dimension"),
						(btn, val) -> dimIndex = val));

		this.targetNameField = addRenderableWidget(new EditBox(this.font,
				cx - fw / 2, 110, fw, 20, Component.translatable("screen.powers.teleport.target")));
		this.targetNameField.setHint(Component.literal("Player name"));

		this.addRenderableWidget(Button.builder(Component.translatable("screen.powers.teleport.go"),
				btn -> confirm()).bounds(cx - 60, 138, 120, 20).build());
	}

	private void confirm() {
		this.error = null;
		try {
			String target = targetNameField != null ? targetNameField.getValue().trim() : "";

			if (mode == 2) {
				if (target.isEmpty()) { error = Component.literal("Enter a player name"); return; }
				// remember the slot for 200 ticks; pressing its key again confirms the teleport
				ClientPowerState.markingSlot = slot;
				ClientPowerState.markingTicks = 200;
				ClientPlayNetworking.send(new PowersPackets.TeleportRequestPayload(
						slot, 0, 0, 0,
						ResourceKey.create(Registries.DIMENSION, Identifier.fromNamespaceAndPath("minecraft", "overworld")),
						target, true));
			} else {
				// modes 0 and 1 use the typed coordinates, mode 1 also names the player being moved
				double x = Double.parseDouble(xField.getValue().trim());
				double y = Double.parseDouble(yField.getValue().trim());
				double z = Double.parseDouble(zField.getValue().trim());
				String dimId = available.get(dimIndex).id();
				String targetName = (mode == 1) ? target : "";
				Identifier id = Identifier.tryParse(dimId);
				ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, id);
				ClientPlayNetworking.send(new PowersPackets.TeleportRequestPayload(
						slot, x, y, z, key, targetName, false));
			}
			this.onClose();
		} catch (NumberFormatException e) {
			// bad numbers just show an error, the screen stays open for a retry
			this.error = Component.translatable("screen.powers.teleport.invalid");
		}
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		super.extractRenderState(g, mouseX, mouseY, delta);
		int cx = this.width / 2;

		g.text(this.font, this.title.getString(), (this.width - this.font.width(this.title)) / 2, 18, 0xFFFFFFFF, true);

		boolean coords = mode != 2;
		String label = mode == 2 ? "Target Player" : (mode == 1 ? "Teleport this player" : "Teleport yourself");
		int lw = this.font.width(label);
		g.text(this.font, label, cx - lw / 2, 136, 0xFFCCCCCC, true);

		if (this.error != null) {
			g.text(this.font, this.error.getString(),
					(cx - this.font.width(this.error)) / 2, this.height - 24, 0xFFFF5555, true);
		}
	}
}