package com.powers.client;

import com.powers.PowersEffects;
import com.powers.PowersMod;
import com.powers.hud.HudEnergyMode;
import com.powers.hud.HudMath;
import com.powers.hud.HudLayout;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/** Renders the texture-backed ancient reliquary used for every energy state. */
public final class EnergyHudRenderer {
	private static final Identifier FRAME = PowersMod.id("textures/gui/energy_frame.png");
	private static final Identifier FILL = PowersMod.id("textures/gui/energy_fill.png");
	private static final int FRAME_WIDTH = 172;
	private static final int FRAME_HEIGHT = 22;
	private static final int FILL_WIDTH = 144;
	private static final int FILL_HEIGHT = 8;

	private EnergyHudRenderer() {
	}

	public static void render(GuiGraphicsExtractor graphics) {
		Minecraft client = Minecraft.getInstance();
		if (client.player == null) return;
		int capacity = ClientPowerState.energyCapacity();
		int energy = Math.max(0, Math.min(capacity, ClientPowerState.energy()));
		HudEnergyMode mode = HudMath.mode(energy,
				client.player.hasEffect(PowersEffects.AMETHYST_POISONING),
				ClientPowerState.darkness(), ClientPowerState.projection());
		HudLayout.Rect bounds = HudLayout.forScreen(client.getWindow().getGuiScaledWidth(),
				client.getWindow().getGuiScaledHeight()).energy();
		int x = bounds.x();
		int y = bounds.y();
		int row = textureRow(mode);
		int filled = HudMath.filledSegments(energy, capacity, FILL_WIDTH);

		graphics.blit(RenderPipelines.GUI_TEXTURED, FRAME, x, y, 0, 0,
				FRAME_WIDTH, FRAME_HEIGHT, FRAME_WIDTH, FRAME_HEIGHT);
		// Empty and dampened are vessel-wide states, so their fracture pattern
		// remains readable even when no usable energy can be drawn.
		if (mode == HudEnergyMode.EMPTY || mode == HudEnergyMode.DAMPENED) {
			graphics.blit(RenderPipelines.GUI_TEXTURED, FILL, x + 14, y + 7, 0, row * FILL_HEIGHT,
					FILL_WIDTH, FILL_HEIGHT, FILL_WIDTH, FILL_HEIGHT, FILL_WIDTH, 40,
					mode == HudEnergyMode.EMPTY ? 0xCCFFFFFF : 0xE6FFFFFF);
		} else if (filled > 0) {
			graphics.blit(RenderPipelines.GUI_TEXTURED, FILL, x + 14, y + 7, 0, row * FILL_HEIGHT,
					filled, FILL_HEIGHT, filled, FILL_HEIGHT, FILL_WIDTH, 40);
		}

		int tick = client.player.tickCount;
		if (filled > 3 && mode != HudEnergyMode.EMPTY && mode != HudEnergyMode.DAMPENED) {
			int shimmer = Math.floorMod(tick / 2, filled);
			graphics.fill(x + 14 + shimmer, y + 8, x + 15 + shimmer, y + 14, 0xAAFFFFFF);
		}

		int percent = (int) Math.round(energy * 100.0 / Math.max(1, capacity));
		String amount = percent + "%";
		graphics.text(client.font, amount, x + FRAME_WIDTH - 12 - client.font.width(amount),
				y - 9, activeColor(mode, tick), true);
		Component state = Component.translatable("hud.powers.energy." + mode.name().toLowerCase());
		graphics.text(client.font, state, x + 12, y - 9, activeColor(mode, tick), true);
		if (mode == HudEnergyMode.DAMPENED) drawAmethystFracture(graphics, x, y, tick);
	}

	private static int textureRow(HudEnergyMode mode) {
		return switch (mode) {
			case NORMAL -> 0;
			case EMPTY -> 1;
			case DAMPENED -> 2;
			case DARKNESS -> 3;
			case PROJECTION -> 4;
		};
	}

	private static int activeColor(HudEnergyMode mode, int tick) {
		return switch (mode) {
			case NORMAL -> 0xFF62E6FF;
			case DARKNESS -> (tick / 8) % 2 == 0 ? 0xFF7650D8 : 0xFFB04BDD;
			case PROJECTION -> (tick / 6) % 2 == 0 ? 0xFFB8F3FF : 0xFF82B7E8;
			case EMPTY -> (tick / 6) % 2 == 0 ? 0xFFCF433D : 0xFFFF795D;
			case DAMPENED -> 0xFFD19AFF;
		};
	}

	private static void drawAmethystFracture(GuiGraphicsExtractor graphics, int x, int y, int tick) {
		int pulse = (tick / 5) % 2 == 0 ? 0xAAB36BFF : 0x775E2A80;
		for (int step = 0; step < 4; step++) {
			graphics.fill(x + 28 + step * 31, y + 4, x + 29 + step * 31, y + 7 + step % 2, pulse);
			graphics.fill(x + 29 + step * 31, y + 7 + step % 2,
					x + 31 + step * 31, y + 9 + step % 3, pulse);
		}
	}
}
