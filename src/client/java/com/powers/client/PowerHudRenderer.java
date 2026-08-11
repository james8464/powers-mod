package com.powers.client;

import com.powers.cooldown.CooldownPresentation;
import com.powers.PowersMod;
import com.powers.hud.HudMath;
import com.powers.hud.HudLayout;
import com.powers.power.Power;
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
		int color = power == null ? 0xFF626874 : 0xFF000000 | power.color();
		boolean activeToggle = power != null && power.ability() != null && power.ability().isToggle()
				&& ClientPowerState.isToggleActive(power.id().toString());
		Identifier texture = activeToggle && (tick / 5) % 2 == 0 ? ACTIVE_SLOT : SLOT;
		graphics.blit(RenderPipelines.GUI_TEXTURED, texture, centerX - SIZE / 2, centerY - SIZE / 2,
				0, 0, SIZE, SIZE, SIZE, SIZE);
		AbilityGlyphRenderer.draw(graphics, power == null ? null : power.id().getPath(), centerX, centerY,
				activeToggle ? 0xFFF5FDFF : color);

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
			AbilityGlyphRenderer.diamond(graphics, centerX - 3, centerY, 2, 0xFFD7F8FF);
			AbilityGlyphRenderer.diamond(graphics, centerX + 3, centerY, 2, 0xFFFFD166);
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
			String seconds = Long.toString(CooldownPresentation.wholeSeconds(remaining));
			graphics.text(font, seconds, centerX - font.width(seconds) / 2,
					centerY + 6, 0xFFE7EBF2, true);
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
