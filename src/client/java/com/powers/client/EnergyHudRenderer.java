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
	private static final Identifier FULL_ICON = PowersMod.id("textures/gui/mana_icon.png");
	private static final Identifier HALF_ICON = PowersMod.id("textures/gui/mana_icon_half.png");
	private static final Identifier EMPTY_ICON = PowersMod.id("textures/gui/mana_icon_off.png");

	private EnergyHudRenderer() {
	}

	public static void render(GuiGraphicsExtractor graphics) {
		Minecraft client = Minecraft.getInstance();
		if (client.player == null) return;

		int width = client.getWindow().getGuiScaledWidth();
		int height = client.getWindow().getGuiScaledHeight();
		int x = width / 2 + 91;
		int y = height - 50;
		int energy = Math.max(0, Math.min(PowerEnergy.MAX, ClientPowerState.energy()));
		boolean dampened = client.player.hasEffect(PowersEffects.AMETHYST_POISONING);

		int full = energy / (PowerEnergy.MAX / SEGMENTS);
		boolean half = energy % (PowerEnergy.MAX / SEGMENTS) >= (PowerEnergy.MAX / SEGMENTS) / 2;
		for (int i = 0; i < SEGMENTS; i++) {
			Identifier icon = dampened ? EMPTY_ICON : i < full ? FULL_ICON : i == full && half ? HALF_ICON : EMPTY_ICON;
			graphics.blit(icon, x + i * 10, y, 9, 9, 0, 2.0f / 3.0f, 1, 1);
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
