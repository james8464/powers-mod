package com.powers.client;

import com.powers.power.PowerEnergy;
import com.powers.PowersMod;
import com.powers.PowersEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/** A segmented energy meter aligned above the vanilla hunger row. */
public final class EnergyHudRenderer {
	private static final int SEGMENTS = 10;

	private EnergyHudRenderer() {
	}

	public static void render(GuiGraphicsExtractor graphics) {
		Minecraft client = Minecraft.getInstance();
		if (client.player == null) return;

		int width = client.getWindow().getGuiScaledWidth();
		int height = client.getWindow().getGuiScaledHeight();
		int x = width / 2 + 91;
		int y = height - 58;
		int energy = Math.max(0, Math.min(PowerEnergy.MAX, ClientPowerState.energy()));
		boolean dampened = client.player.hasEffect(PowersEffects.AMETHYST_POISONING);

		int full = energy / (PowerEnergy.MAX / SEGMENTS);
		boolean half = energy % (PowerEnergy.MAX / SEGMENTS) >= (PowerEnergy.MAX / SEGMENTS) / 2;
		StringBuilder glyphs = new StringBuilder(SEGMENTS);
		for (int i = 0; i < SEGMENTS; i++) {
			char glyph = dampened ? '\uE002' : i < full ? '\uE000' : i == full && half ? '\uE001' : '\uE002';
			glyphs.append(glyph);
		}
		Component bar = Component.literal(glyphs.toString())
				.withStyle(style -> style.withFont(new net.minecraft.network.chat.FontDescription.Resource(
						PowersMod.id("mana"))));
		graphics.text(client.font, bar, x, y, 0xFFFFFFFF, false);
		if (dampened) {
			int purple = 0xCCB36BFF;
			graphics.fill(0, 0, width, 2, purple);
			graphics.fill(0, height - 2, width, height, purple);
			graphics.fill(0, 0, 2, height, purple);
			graphics.fill(width - 2, 0, width, height, purple);
		}
	}
}
