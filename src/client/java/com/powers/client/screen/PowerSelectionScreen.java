package com.powers.client.screen;

import com.powers.network.PowersPackets;
import com.powers.power.Ability;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.stream.IntStream;

/** Small vanilla-styled menu for server-authoritative power option selection. */
public final class PowerSelectionScreen extends Screen {
	private final int slot;
	private final Ability ability;
	private final int initialOption;
	private int selectedOption;

	public PowerSelectionScreen(int slot, Ability ability, int initialOption) {
		super(ability.name());
		this.slot = slot;
		this.ability = ability;
		this.initialOption = initialOption;
	}

	@Override
	protected void init() {
		int count = ability.selectionOptionCount();
		selectedOption = Math.floorMod(initialOption, count);
		List<Integer> options = IntStream.range(0, count).boxed().toList();
		addRenderableWidget(CycleButton.<Integer>builder(
				ability::selectionOptionName, () -> selectedOption)
				.withValues(options).displayOnlyValue()
				.create(width / 2 - 80, height / 2 - 16, 160, 20,
						Component.literal("Selection"),
						(button, option) -> selectedOption = option));
		addRenderableWidget(Button.builder(Component.literal("Select"), button -> choose())
				.bounds(width / 2 - 60, height / 2 + 16, 120, 20).build());
	}

	private void choose() {
		ClientPlayNetworking.send(new PowersPackets.SelectAbilityOptionPayload(slot, selectedOption));
		onClose();
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);
		graphics.centeredText(font, title, width / 2, height / 2 - 42, 0xFFFFFFFF);
	}
}
