package com.powers.client;

import com.powers.PowersEffects;
import com.powers.PowersMod;
import com.powers.hud.HudEnergyMode;
import com.powers.hud.HudMath;
import com.powers.hud.HudLayout;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/** Renders ten vanilla-scale full/half energy symbols above the hunger row. */
public final class EnergyHudRenderer {
	private static final Identifier SYMBOLS = PowersMod.id("textures/gui/energy_symbols.png");
	private static final int SYMBOL_SIZE = 9;
	private static final int SYMBOL_STEP = 8;
	private static final int TEXTURE_WIDTH = 27;
	private static final int TEXTURE_HEIGHT = 45;

	private EnergyHudRenderer() {
	}

	public static void render(GuiGraphicsExtractor graphics) {
		Minecraft client = Minecraft.getInstance();
		if (client.player == null) return;
		int capacity = ClientPowerState.energyCapacity();
		int energy = Math.max(0, Math.min(capacity, ClientPowerState.energy()));
		HudEnergyMode mode = HudMath.mode(energy,
				client.player.hasEffect(PowersEffects.AMETHYST_POISONING),
				ClientPowerState.darkness(), ClientPowerState.projection());
		HudLayout.Rect bounds = HudLayout.forScreen(client.getWindow().getGuiScaledWidth(),
				client.getWindow().getGuiScaledHeight()).energy();
		int halfUnits = mode == HudEnergyMode.EMPTY ? 0 : HudMath.energyHalfUnits(energy, capacity);
		int visibleSymbols = Math.min(10, Math.max(0, (bounds.width() + SYMBOL_STEP - 1) / SYMBOL_STEP));
		for (int symbol = 0; symbol < visibleSymbols; symbol++) {
			int threshold = symbol * 2;
			int sourceX = halfUnits >= threshold + 2 ? SYMBOL_SIZE * 2
					: halfUnits == threshold + 1 ? SYMBOL_SIZE : 0;
			int x = bounds.x() + bounds.width() - SYMBOL_SIZE - symbol * SYMBOL_STEP;
			graphics.blit(RenderPipelines.GUI_TEXTURED, SYMBOLS, x, bounds.y(), sourceX,
					textureRow(mode) * SYMBOL_SIZE, SYMBOL_SIZE, SYMBOL_SIZE,
					TEXTURE_WIDTH, TEXTURE_HEIGHT);
		}
	}

	private static int textureRow(HudEnergyMode mode) {
		return switch (mode) {
			case NORMAL -> 0;
			case EMPTY -> 1;
			case DAMPENED -> 2;
			case DARKNESS -> 3;
			case PROJECTION -> 4;
		};
	}

}
