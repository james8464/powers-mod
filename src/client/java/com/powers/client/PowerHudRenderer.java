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

/** Renders the compact, icon-first vertical power rail. */
public final class PowerHudRenderer {
	private static final Identifier SLOT = PowersMod.id("textures/gui/power_slot.png");
	private static final Identifier ACTIVE_SLOT = PowersMod.id("textures/gui/power_slot_active.png");
	private static final int SIZE = HudLayout.POWER_SLOT_SIZE;
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
		drawPowerGlyph(graphics, power, centerX, centerY,
				activeToggle ? 0xFFF5FDFF : color);
		if (elemental != null) drawElementalCycle(graphics, centerX, centerY - 9, elemental, tick);

		int remaining = ClientPowerState.cooldownTicks(slot);
		int reactivation = ClientPowerState.reactivationTicks(slot);
		int maximum = Math.max(1, ClientPowerState.cooldownMaximum(slot));
		int blockedRunes = HudMath.cooldownSegments(remaining, maximum, COOLDOWN_RUNES);
		for (int rune = 0; rune < COOLDOWN_RUNES; rune++) {
			double angle = -Math.PI / 2.0 + rune * Math.PI * 2.0 / COOLDOWN_RUNES;
			int runeX = centerX + (int) Math.round(Math.cos(angle) * 12.0);
			int runeY = centerY + (int) Math.round(Math.sin(angle) * 12.0);
			int runeColor = reactivation > 0 ? HudMath.secondStepRuneColor(rune, tick)
					: rune < blockedRunes ? 0xDD424752 : activeToggle ? 0xFFFFFFFF : color;
			graphics.fill(runeX - 1, runeY - 1, runeX + 2, runeY + 2, runeColor);
		}
		if (reactivation > 0) {
			drawDiamond(graphics, centerX - 3, centerY, 2, 0xFFD7F8FF);
			drawDiamond(graphics, centerX + 3, centerY, 2, 0xFFFFD166);
		}

		String key = keyLabel(slot);
		graphics.text(font, key, centerX - SIZE / 2 - font.width(key) - 3,
				centerY - 4, 0xFFFFFFFF, true);
		if (reactivation > 0) {
			String marker = net.minecraft.network.chat.Component.translatable(
					"hud.powers.second_step").getString();
			graphics.text(font, marker, centerX - font.width(marker) / 2,
					centerY + 6, 0xFFFFE6A3, true);
		} else if (remaining > 0) {
			String seconds = String.valueOf((remaining + 19) / 20);
			graphics.text(font, seconds, centerX - font.width(seconds) / 2,
					centerY + 6, 0xFFE7EBF2, true);
		}
	}

	/** Draws a readable semantic sigil without requiring one texture per power. */
	private static void drawPowerGlyph(GuiGraphicsExtractor graphics, Power power,
			int centerX, int centerY, int color) {
		if (power == null) {
			drawDiamond(graphics, centerX, centerY, 2, 0xFF626874);
			return;
		}
		String id = power.id().getPath();
		if (id.contains("lightning")) {
			graphics.fill(centerX, centerY - 6, centerX + 2, centerY - 1, color);
			graphics.fill(centerX - 2, centerY - 1, centerX + 2, centerY + 2, color);
			graphics.fill(centerX - 2, centerY + 2, centerX, centerY + 7, color);
		} else if (id.contains("fire")) {
			drawDiamond(graphics, centerX, centerY + 2, 5, color);
			drawDiamond(graphics, centerX + 1, centerY - 4, 3, color);
		} else if (id.contains("flight") || id.contains("breezy")) {
			for (int step = 0; step < 5; step++) {
				graphics.fill(centerX - 7 + step, centerY - 3 + step,
						centerX - 5 + step, centerY + 3 + step, color);
				graphics.fill(centerX + 5 - step, centerY - 3 + step,
						centerX + 7 - step, centerY + 3 + step, color);
			}
		} else if (id.contains("time")) {
			graphics.fill(centerX - 5, centerY - 6, centerX + 6, centerY - 4, color);
			graphics.fill(centerX - 5, centerY + 4, centerX + 6, centerY + 6, color);
			for (int step = 0; step < 4; step++) {
				graphics.fill(centerX - 3 + step, centerY - 4 + step,
						centerX + 4 - step, centerY - 3 + step, color);
				graphics.fill(centerX - step, centerY + step,
						centerX + step + 1, centerY + step + 1, color);
			}
		} else if (id.contains("ice") || id.contains("frost")) {
			graphics.fill(centerX, centerY - 7, centerX + 1, centerY + 8, color);
			graphics.fill(centerX - 7, centerY, centerX + 8, centerY + 1, color);
			for (int offset = -5; offset <= 5; offset++) {
				graphics.fill(centerX + offset, centerY + offset,
						centerX + offset + 1, centerY + offset + 1, color);
				graphics.fill(centerX + offset, centerY - offset,
						centerX + offset + 1, centerY - offset + 1, color);
			}
		} else if (id.contains("shadow") || id.contains("void") || id.contains("invisibility")) {
			drawDiamond(graphics, centerX, centerY, 7, color);
			drawDiamond(graphics, centerX + 3, centerY - 2, 6, 0xFF11131B);
		} else if (id.contains("plant") || id.contains("health") || id.contains("healing")) {
			graphics.fill(centerX - 2, centerY - 7, centerX + 3, centerY + 8, color);
			graphics.fill(centerX - 7, centerY - 2, centerX + 8, centerY + 3, color);
		} else if (id.contains("speed")) {
			for (int arrow = -1; arrow <= 1; arrow++) {
				int y = centerY + arrow * 5;
				graphics.fill(centerX - 6, y, centerX + 5, y + 2, color);
				graphics.fill(centerX + 2, y - 2, centerX + 7, y + 4, color);
			}
		} else {
			drawDiamond(graphics, centerX, centerY, 6, color);
			drawDiamond(graphics, centerX, centerY, 2, 0xFF11131B);
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

}
