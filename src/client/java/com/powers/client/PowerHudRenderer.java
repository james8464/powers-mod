package com.powers.client;

import com.powers.power.Power;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** the three slot boxes in the corner of the screen, each one showing which power its key triggers */
public final class PowerHudRenderer {
	private static final int BOX = 40;
	private static final int GAP = 4;
	private static final int MARGIN = 4;

	private PowerHudRenderer() {
	}

	public static void render(GuiGraphicsExtractor graphics, DeltaTracker tickCounter) {
		var client = Minecraft.getInstance();
		if (client.player == null) {
			return;
		}
		var font = client.font;
		int sw = client.getWindow().getGuiScaledWidth();
		int sh = client.getWindow().getGuiScaledHeight();

		// sits just above the hotbar in the bottom-right corner
		int totalW = 3 * BOX + 2 * GAP;
		int x0 = sw - totalW - MARGIN;
		int y0 = sh - BOX - 14 - MARGIN;

		for (int slot = 0; slot < 3; slot++) {
			Power power = ClientPowerState.getPower(slot);
			int x = x0 + slot * (BOX + GAP);
			int y = y0;

			graphics.fill(x, y, x + BOX, y + BOX, 0x80000000);
			graphics.fill(x, y, x + BOX + 1, y + 1, 0x44FFFFFF);
			graphics.fill(x, y + BOX, x + BOX + 1, y + BOX + 1, 0x44FFFFFF);
			graphics.fill(x, y, x + 1, y + BOX + 1, 0x44FFFFFF);
			graphics.fill(x + BOX, y, x + BOX + 1, y + BOX + 1, 0x44FFFFFF);

			int fillColor;
			// toggle powers glow in their color while switched on, empty slots stay dim
			if (power == null) {
				fillColor = 0x44FFFFFF;
			} else if (power.ability() != null && power.ability().isToggle()) {
				boolean on = ClientPowerState.isToggleActive(power.id().toString());
				fillColor = on ? (0xFF000000 | power.color()) : 0x44FFFFFF;
			} else {
				fillColor = 0xFF000000 | power.color();
			}

			graphics.fill(x, y, x + BOX, y + BOX, fillColor);

			String key = keyLabel(slot);
			int kw = font.width(key);
			graphics.text(font, key, x + (BOX - kw) / 2, y + (BOX - 9) / 2, 0xFFFFFFFF, true);

			String label;
			int labelColor;
			if (power == null) {
				label = "--";
				labelColor = 0xFF666666;
			} else {
				label = truncate(font, power.name().getString(), BOX);
				labelColor = 0xFFAAAAAA;
			}
			int lw = font.width(label);
			graphics.text(font, label, x + (BOX - lw) / 2, y + BOX + 2, labelColor, true);
		}
	}

	private static String keyLabel(int slot) {
		return switch (slot) {
			case 0 -> PowersClient.slotKey1.getTranslatedKeyMessage().getString();
			case 1 -> PowersClient.slotKey2.getTranslatedKeyMessage().getString();
			default -> PowersClient.slotKey3.getTranslatedKeyMessage().getString();
		};
	}

	private static String truncate(net.minecraft.client.gui.Font font, String text, int maxWidth) {
		if (font.width(text) <= maxWidth) {
			return text;
		}
		// trim the name until it fits the box width, with ".." as the ellipsis
		String s = text;
		while (font.width(s + "..") > maxWidth && s.length() > 1) {
			s = s.substring(0, s.length() - 1);
		}
		return s + "..";
	}
}
