package com.powers.client;

import com.powers.PowersMod;
import com.powers.hud.HudMath;
import com.powers.hud.HudLayout;
import com.powers.power.Power;
import com.powers.power.abilities.ElementalPhase;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

import java.util.Locale;

/** Renders three compact rune medallions with readable cooldown rings. */
public final class PowerHudRenderer {
	private static final Identifier SLOT = PowersMod.id("textures/gui/power_slot.png");
	private static final Identifier ACTIVE_SLOT = PowersMod.id("textures/gui/power_slot_active.png");
	private static final int SIZE = 36;
	private static final int COOLDOWN_RUNES = 12;

	private PowerHudRenderer() {
	}

	public static void render(GuiGraphicsExtractor graphics, DeltaTracker tickCounter) {
		Minecraft client = Minecraft.getInstance();
		if (client.player == null) return;
		HudLayout layout = HudLayout.forScreen(client.getWindow().getGuiScaledWidth(),
				client.getWindow().getGuiScaledHeight());
		for (int slot = 0; slot < 3; slot++) {
			HudLayout.Rect bounds = layout.powerSlots().get(slot);
			drawSlot(graphics, client.font, slot, bounds.x() + SIZE / 2,
					bounds.y() + SIZE / 2, client.player.tickCount);
		}
	}

	private static void drawSlot(GuiGraphicsExtractor graphics, Font font, int slot,
			int centerX, int centerY, int tick) {
		Power power = ClientPowerState.getPower(slot);
		ElementalPhase elemental = power != null && power.id().getPath().equals("elemental_blast")
				? ElementalPhase.fromIndex(ClientPowerState.elementalPhase()) : null;
		int color = power == null ? 0xFF626874
				: 0xFF000000 | (elemental == null ? power.color() : elemental.color());
		boolean activeToggle = power != null && power.ability() != null && power.ability().isToggle()
				&& ClientPowerState.isToggleActive(power.id().toString());
		Identifier texture = activeToggle && (tick / 5) % 2 == 0 ? ACTIVE_SLOT : SLOT;
		graphics.blit(RenderPipelines.GUI_TEXTURED, texture, centerX - SIZE / 2, centerY - SIZE / 2,
				0, 0, SIZE, SIZE, SIZE, SIZE);
		drawDiamond(graphics, centerX, centerY, 5, activeToggle ? 0xFFF5FDFF : color);
		if (elemental != null) drawElementalCycle(graphics, centerX, centerY - 10, elemental, tick);

		int remaining = ClientPowerState.cooldownTicks(slot);
		int maximum = Math.max(1, ClientPowerState.cooldownMaximum(slot));
		int blockedRunes = HudMath.cooldownSegments(remaining, maximum, COOLDOWN_RUNES);
		for (int rune = 0; rune < COOLDOWN_RUNES; rune++) {
			double angle = -Math.PI / 2.0 + rune * Math.PI * 2.0 / COOLDOWN_RUNES;
			int runeX = centerX + (int) Math.round(Math.cos(angle) * 15.0);
			int runeY = centerY + (int) Math.round(Math.sin(angle) * 15.0);
			int runeColor = rune < blockedRunes ? 0xDD424752 : activeToggle ? 0xFFFFFFFF : color;
			graphics.fill(runeX - 1, runeY - 1, runeX + 2, runeY + 2, runeColor);
		}

		String key = keyLabel(slot);
		graphics.text(font, key, centerX - font.width(key) / 2, centerY - 4, 0xFFFFFFFF, true);
		if (power != null) {
			String name = power.name().getString();
			if (elemental != null) {
				String phase = net.minecraft.network.chat.Component.translatable(
						"hud.powers.element." + elemental.name().toLowerCase(Locale.ROOT)).getString();
				name += " · " + phase;
			}
			String label = trim(font, name, 86);
			graphics.text(font, label, centerX - 22 - font.width(label), centerY - 4, color, true);
		}
		if (remaining > 0) {
			String seconds = String.valueOf((remaining + 19) / 20);
			graphics.text(font, seconds, centerX - font.width(seconds) / 2,
					centerY + 6, 0xFFE7EBF2, true);
		}
	}

	private static void drawElementalCycle(GuiGraphicsExtractor graphics, int centerX, int centerY,
			ElementalPhase current, int tick) {
		for (int index = 0; index < ElementalPhase.values().length; index++) {
			int x = centerX - 9 + index * 6;
			int radius = index == current.index() ? 2 : 1;
			drawDiamond(graphics, x, centerY, radius,
					HudMath.elementalRuneColor(current.index(), index, tick));
		}
	}

	private static void drawDiamond(GuiGraphicsExtractor graphics, int centerX, int centerY,
			int radius, int color) {
		for (int dy = -radius; dy <= radius; dy++) {
			int half = radius - Math.abs(dy);
			graphics.fill(centerX - half, centerY + dy,
					centerX + half + 1, centerY + dy + 1, color);
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
