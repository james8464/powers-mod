package com.powers.client;

import com.powers.power.PowerEnergy;
import com.powers.PowersMod;
import com.powers.PowersEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

/** the energy meter above the hunger row: ten segments showing how much power you have stored */
public final class EnergyHudRenderer {
	private static final int SEGMENTS = 10;
	private static final Identifier FULL_ICON = PowersMod.id("custom_icon.png");
	private static final Identifier HALF_ICON = PowersMod.id("custom_icon_half.png");
	private static final Identifier EMPTY_ICON = PowersMod.id("custom_icon_off.png");

	private EnergyHudRenderer() {
	}

	public static void render(GuiGraphicsExtractor graphics) {
		Minecraft client = Minecraft.getInstance();
		if (client.player == null) return;

		int width = client.getWindow().getGuiScaledWidth();
		int height = client.getWindow().getGuiScaledHeight();
		// the vanilla hunger bar sits at y = height - 48, so the meter draws just above it
		int y = height - 58;
		int capacity = ClientPowerState.energyCapacity();
		int energy = Math.max(0, Math.min(capacity, ClientPowerState.energy()));
		boolean dampened = client.player.hasEffect(PowersEffects.AMETHYST_POISONING);

		int full = energy * SEGMENTS / capacity;
		// a leftover of at least half a segment shows as a half-filled icon
		boolean half = energy * SEGMENTS % capacity >= capacity / (SEGMENTS * 2);
		for (int hungerIndex = 0; hungerIndex < SEGMENTS; hungerIndex++) {
			// fill from the right like vanilla hunger, so the meter empties toward the left
			int displayIndex = SEGMENTS - 1 - hungerIndex;
			int empty = SEGMENTS - full - (half ? 1 : 0);
			Identifier icon = dampened || displayIndex < empty ? EMPTY_ICON
					: half && displayIndex == empty ? HALF_ICON : FULL_ICON;
			int x = width / 2 + 82 - hungerIndex * 8;
			graphics.blit(icon, x, y, 9, 9, 0, 2.0f / 3.0f, 1, 1);
		}
		// amethyst poisoning drains the meter visually and frames the screen in purple
		if (dampened) {
			int purple = 0xCCB36BFF;
			graphics.fill(0, 0, width, 2, purple);
			graphics.fill(0, height - 2, width, height, purple);
			graphics.fill(0, 0, 2, height, purple);
			graphics.fill(width - 2, 0, width, height, purple);
		}
	}
}
