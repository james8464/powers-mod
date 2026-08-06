package com.powers.client;

import com.powers.power.PowerEnergy;
import com.powers.PowersMod;
import com.powers.PowersEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

/** A segmented energy meter aligned above the vanilla hunger row. */
public final class EnergyHudRenderer {
	private static final int SEGMENTS = 10;
	private static final Identifier HUD_BAR_TEXTURE = PowersMod.id("textures/imported/gui/hud_icons_unknown.png");

	private EnergyHudRenderer() {
	}

	public static void render(GuiGraphicsExtractor graphics) {
		Minecraft client = Minecraft.getInstance();
		if (client.player == null) return;

		int width = client.getWindow().getGuiScaledWidth();
		int height = client.getWindow().getGuiScaledHeight();
		int x = width / 2 + 91;
		int y = height - 50;
		int segmentWidth = 8;
		int gap = 1;
		int totalWidth = SEGMENTS * segmentWidth + (SEGMENTS - 1) * gap;
		int energy = Math.max(0, Math.min(PowerEnergy.MAX, ClientPowerState.energy()));
		boolean dampened = client.player.hasEffect(PowersEffects.AMETHYST_POISONING);

		graphics.blit(HUD_BAR_TEXTURE, x - 2, y - 2, totalWidth + 4, 10, 0.5f, 0, 1, 1);
		int filled = (int) Math.ceil(energy * SEGMENTS / (double) PowerEnergy.MAX);
		for (int i = filled; i < SEGMENTS; i++) {
			int sx = x + i * (segmentWidth + gap);
			graphics.fill(sx, y, sx + segmentWidth, y + 6, dampened ? 0xAA3B174D : 0xAA081317);
		}
		if (dampened) {
			int purple = 0xCCB36BFF;
			graphics.fill(0, 0, width, 2, purple);
			graphics.fill(0, height - 2, width, height, purple);
			graphics.fill(0, 0, 2, height, purple);
			graphics.fill(width - 2, 0, width, height, purple);
		}
	}
}
