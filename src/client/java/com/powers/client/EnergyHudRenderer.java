package com.powers.client;

import com.powers.PowersEffects;
import com.powers.hud.HudEnergyMode;
import com.powers.hud.HudMath;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/** A segmented magical arc around the hotbar, with distinct energy corruption states. */
public final class EnergyHudRenderer {
	private static final int SEGMENTS = 24;

	private EnergyHudRenderer() {
	}

	public static void render(GuiGraphicsExtractor graphics) {
		Minecraft client = Minecraft.getInstance();
		if (client.player == null) return;
		int width = client.getWindow().getGuiScaledWidth();
		int height = client.getWindow().getGuiScaledHeight();
		int capacity = ClientPowerState.energyCapacity();
		int energy = Math.max(0, Math.min(capacity, ClientPowerState.energy()));
		boolean dampened = client.player.hasEffect(PowersEffects.AMETHYST_POISONING);
		HudEnergyMode mode = HudMath.mode(energy, dampened, ClientPowerState.darkness());
		int filled = HudMath.filledSegments(energy, capacity, SEGMENTS);
		int tick = client.player.tickCount;

		int centerX = width / 2;
		int centerY = height - 27;
		int radius = 36;
		int active = activeColor(mode, tick);
		int dormant = mode == HudEnergyMode.DARKNESS ? 0x55391F56 : 0x55253A42;
		for (int index = 0; index < SEGMENTS; index++) {
			double angle = Math.PI + Math.PI * (index + 0.5) / SEGMENTS;
			int x = centerX + (int) Math.round(Math.cos(angle) * radius);
			int y = centerY + (int) Math.round(Math.sin(angle) * radius * 0.72);
			boolean lit = index < filled && mode != HudEnergyMode.DAMPENED;
			int color = lit ? active : dormant;
			if (mode == HudEnergyMode.DAMPENED && index < filled && (index + tick / 3) % 3 != 0) {
				color = 0xCCB36BFF;
			}
			int segmentHeight = index < 3 || index > SEGMENTS - 4 ? 2 : 3;
			graphics.fill(x - 1, y - segmentHeight, x + 2, y + 1, color);
			if (lit && (index + tick) % 11 == 0) graphics.fill(x, y - segmentHeight - 1, x + 1, y, 0xFFFFFFFF);
		}

		drawDiamond(graphics, centerX, centerY - 2, 7, 0xAA05070A);
		drawDiamond(graphics, centerX, centerY - 2, 4, active);
		String amount = String.valueOf(Math.round(energy * 100.0 / Math.max(1, capacity)));
		int amountWidth = client.font.width(amount);
		graphics.text(client.font, amount, centerX - amountWidth / 2, centerY - 7, 0xFFFFFFFF, true);
		Component state = Component.translatable("hud.powers.energy." + mode.name().toLowerCase());
		int stateWidth = client.font.width(state);
		graphics.text(client.font, state, centerX - stateWidth / 2, centerY - 17, active, true);

		if (mode == HudEnergyMode.DAMPENED) drawAmethystFracture(graphics, width, height, tick);
	}

	private static int activeColor(HudEnergyMode mode, int tick) {
		return switch (mode) {
			case NORMAL -> 0xFF62E6FF;
			case DARKNESS -> (tick / 8) % 2 == 0 ? 0xFF7650D8 : 0xFFB04BDD;
			case EMPTY -> (tick / 6) % 2 == 0 ? 0xFF7A2530 : 0xFFB93B45;
			case DAMPENED -> 0xFFB36BFF;
		};
	}

	private static void drawDiamond(GuiGraphicsExtractor graphics, int cx, int cy, int radius, int color) {
		for (int dy = -radius; dy <= radius; dy++) {
			int half = radius - Math.abs(dy);
			graphics.fill(cx - half, cy + dy, cx + half + 1, cy + dy + 1, color);
		}
	}

	private static void drawAmethystFracture(GuiGraphicsExtractor graphics, int width, int height, int tick) {
		int pulse = (tick / 5) % 2 == 0 ? 0x88B36BFF : 0x665E2A80;
		for (int step = 0; step < 5; step++) {
			int offset = step * 5;
			graphics.fill(offset, 0, offset + 2, 9 - step, pulse);
			graphics.fill(width - offset - 2, height - 9 + step, width - offset, height, pulse);
		}
	}
}
