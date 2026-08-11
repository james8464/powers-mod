package com.powers.client.screen;

import com.powers.network.RelicPackets;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Exact server-authored reservoir balances with direction-only transfer controls. */
public final class ReservoirTransferScreen extends Screen {
	private final RelicPackets.OpenReservoirPayload state;

	public ReservoirTransferScreen(RelicPackets.OpenReservoirPayload state) {
		super(Component.translatable("screen.powers.reservoir.title"));
		this.state = state;
	}

	@Override
	protected void init() {
		int left = width / 2 - 104;
		int y = height / 2 + 24;
		Button store = addRenderableWidget(Button.builder(
				Component.translatable("screen.powers.reservoir.store"), ignored -> transfer(false))
				.bounds(left, y, 100, 20).build());
		store.active = state.mainEnergy() > 0 && state.auxiliaryEnergy() < state.auxiliaryCapacity();
		Button release = addRenderableWidget(Button.builder(
				Component.translatable("screen.powers.reservoir.release"), ignored -> transfer(true))
				.bounds(left + 108, y, 100, 20).build());
		release.active = state.auxiliaryEnergy() > 0 && state.mainEnergy() < state.mainCapacity();
		setInitialFocus(store.active ? store : release);
	}

	private void transfer(boolean release) {
		ClientPlayNetworking.send(new RelicPackets.TransferReservoirPayload(state.slot(), release));
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics,
			int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);
		int center = width / 2;
		int top = height / 2 - 52;
		graphics.centeredText(font, title, center, top, 0xFFD7A7FF);
		graphics.centeredText(font, Component.translatable("screen.powers.reservoir.main",
				state.mainEnergy(), state.mainCapacity()), center, top + 18, 0xFFFFFFFF);
		graphics.centeredText(font, Component.translatable("screen.powers.reservoir.auxiliary",
				state.auxiliaryEnergy(), state.auxiliaryCapacity()), center, top + 31, 0xFFFFFFFF);
		graphics.centeredText(font, Component.translatable("screen.powers.reservoir.pending",
				state.pendingCost(), state.pendingShortfall()), center, top + 48,
				state.pendingShortfall() == 0 ? 0xFF9CFFBE : 0xFFFF8C8C);
	}
}
