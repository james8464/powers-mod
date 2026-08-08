package com.powers.client;

import com.powers.hud.HudMath;
import com.powers.power.Power;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Three compact rune-glyphs: colored diamonds, toggle pulses, and cooldown runes. */
public final class PowerHudRenderer {
	private static final int RADIUS = 11;
	private static final int[] RUNE_X = {0, 7, 10, 7, 0, -7, -10, -7};
	private static final int[] RUNE_Y = {-13, -9, 0, 9, 13, 9, 0, -9};

	private PowerHudRenderer() {
	}

	public static void render(GuiGraphicsExtractor graphics, DeltaTracker tickCounter) {
		Minecraft client = Minecraft.getInstance();
		if (client.player == null) return;
		int width = client.getWindow().getGuiScaledWidth();
		int height = client.getWindow().getGuiScaledHeight();
		int x = width - 18;
		int firstY = height - 56;
		for (int slot = 0; slot < 3; slot++) {
			drawSlot(graphics, client.font, slot, x, firstY - slot * 31, client.player.tickCount);
		}
	}

	private static void drawSlot(GuiGraphicsExtractor graphics, Font font, int slot, int cx, int cy, int tick) {
		Power power = ClientPowerState.getPower(slot);
		int color = power == null ? 0xFF50545A : 0xFF000000 | power.color();
		boolean activeToggle = power != null && power.ability() != null && power.ability().isToggle()
				&& ClientPowerState.isToggleActive(power.id().toString());
		int glow = activeToggle && (tick / 5) % 2 == 0 ? 0xEE000000 | power.color() : 0xAA000000 | color;
		drawDiamond(graphics, cx, cy, RADIUS + 2, 0xB805070A);
		drawDiamond(graphics, cx, cy, RADIUS, glow);
		drawDiamond(graphics, cx, cy, 6, 0xB8101218);

		int remaining = ClientPowerState.cooldownTicks(slot);
		int maximum = power == null || power.ability() == null ? 1 : Math.max(1, power.ability().cooldownTicks());
		int cooldownRunes = HudMath.cooldownSegments(remaining, maximum, RUNE_X.length);
		for (int rune = 0; rune < RUNE_X.length; rune++) {
			int runeColor = rune < cooldownRunes ? 0xDD555B66 : (activeToggle ? 0xFFFFFFFF : color);
			graphics.fill(cx + RUNE_X[rune] - 1, cy + RUNE_Y[rune] - 1,
					cx + RUNE_X[rune] + 2, cy + RUNE_Y[rune] + 2, runeColor);
		}

		String key = keyLabel(slot);
		graphics.text(font, key, cx - font.width(key) / 2, cy - 4, 0xFFFFFFFF, true);
		if (power != null) {
			String label = trim(font, power.name().getString(), 76);
			int labelX = cx - 18 - font.width(label);
			graphics.text(font, label, labelX, cy - 8, color, true);
		}
		if (remaining > 0) {
			String seconds = String.valueOf((remaining + 19) / 20);
			graphics.text(font, seconds, cx - font.width(seconds) / 2, cy + 5, 0xFFE0E4EA, true);
		} else if (activeToggle) {
			graphics.text(font, "•", cx - font.width("•") / 2, cy + 5, 0xFFFFFFFF, true);
		}
	}

	private static void drawDiamond(GuiGraphicsExtractor graphics, int cx, int cy, int radius, int color) {
		for (int dy = -radius; dy <= radius; dy++) {
			int half = radius - Math.abs(dy);
			graphics.fill(cx - half, cy + dy, cx + half + 1, cy + dy + 1, color);
		}
	}

	private static String keyLabel(int slot) {
		return switch (slot) {
			case 0 -> PowersClient.slotKey1.getTranslatedKeyMessage().getString();
			case 1 -> PowersClient.slotKey2.getTranslatedKeyMessage().getString();
			default -> PowersClient.slotKey3.getTranslatedKeyMessage().getString();
		};
	}

	private static String trim(Font font, String text, int width) {
		if (font.width(text) <= width) return text;
		String result = text;
		while (result.length() > 1 && font.width(result + "…") > width) {
			result = result.substring(0, result.length() - 1);
		}
		return result + "…";
	}
}
