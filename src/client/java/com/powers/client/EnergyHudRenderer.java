package com.powers.client;

import com.powers.PowersEffects;
import com.powers.PowersMod;
import com.powers.hud.HudEnergyMode;
import com.powers.hud.HudMath;
import com.powers.hud.HudLayout;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;

/** Renders ten vanilla-scale full/half energy symbols above the hunger row. */
public final class EnergyHudRenderer {
	private static final Identifier SYMBOLS = PowersMod.id("textures/gui/energy_symbols.png");
	private static final int SYMBOL_SIZE = 9;
	private static final int TEXTURE_WIDTH = 27;
	private static final int TEXTURE_HEIGHT = 45;

	private EnergyHudRenderer() {
	}

	public static void render(GuiGraphicsExtractor graphics) {
		Minecraft client = Minecraft.getInstance();
		if (!com.powers.hud.HudVisibility.energy(client.player != null,
				client.player != null && client.player.isSpectator(), false)) return;
		int capacity = ClientPowerState.energyCapacity();
		int energy = Math.max(0, Math.min(capacity, ClientPowerState.energy()));
		HudEnergyMode mode = HudMath.mode(energy,
				client.player.hasEffect(PowersEffects.AMETHYST_POISONING),
				ClientPowerState.darkness(), ClientPowerState.projection());
		int airRows = client.player.getAirSupply() < client.player.getMaxAirSupply() ? 1 : 0;
		int mountRows = client.player.getVehicle() instanceof LivingEntity mount
				? Math.clamp((int) Math.ceil(mount.getMaxHealth() / 20.0), 1, 3) : 0;
		HudLayout.Rect bounds = HudLayout.forScreen(client.getWindow().getGuiScaledWidth(),
				client.getWindow().getGuiScaledHeight(), airRows, mountRows,
				ClientHudPreferences.get()).energy();
		int halfUnits = mode == HudEnergyMode.EMPTY ? 0 : HudMath.energyHalfUnits(energy, capacity);
		int visibleSymbols = Math.min(10, Math.max(0, (bounds.width() + 7) / 8));
		boolean atlasAvailable = client.getResourceManager().getResource(SYMBOLS).isPresent();
		for (int symbol = 0; symbol < visibleSymbols; symbol++) {
			int fill = HudMath.energyFillColumn(halfUnits, symbol);
			int x = HudLayout.energySymbolX(bounds, symbol);
			if (com.powers.hud.EnergyAssetDecision.resolve(atlasAvailable)
					== com.powers.hud.EnergyAssetDecision.AUTHORED_ATLAS) {
				graphics.blit(RenderPipelines.GUI_TEXTURED, SYMBOLS, x, bounds.y(), fill * SYMBOL_SIZE,
						textureRow(mode) * SYMBOL_SIZE, SYMBOL_SIZE, SYMBOL_SIZE,
						TEXTURE_WIDTH, TEXTURE_HEIGHT);
			} else {
				drawProceduralSymbol(graphics, x, bounds.y(), fill, mode);
			}
		}
		int mouseX = (int) (client.mouseHandler.xpos() * client.getWindow().getGuiScaledWidth()
				/ client.getWindow().getScreenWidth());
		int mouseY = (int) (client.mouseHandler.ypos() * client.getWindow().getGuiScaledHeight()
				/ client.getWindow().getScreenHeight());
		if (mouseX >= bounds.x() && mouseX < bounds.right()
				&& mouseY >= bounds.y() && mouseY < bounds.bottom()) {
			java.util.List<Component> tooltip = new java.util.ArrayList<>();
			tooltip.add(Component.translatable("tooltip.powers.energy_history",
					ClientPowerState.energyConsumed(), ClientPowerState.energyRestored()));
			var sources = ClientPowerState.energySources();
			var names = com.powers.player.EnergyHistorySource.values();
			for (int index = 0; index < Math.min(sources.size(), names.length); index++) {
				if (sources.get(index) > 0L) tooltip.add(Component.translatable(
						"tooltip.powers.energy_source", names[index].name().toLowerCase(java.util.Locale.ROOT),
						sources.get(index)));
			}
			graphics.setComponentTooltipForNextFrame(client.font, tooltip, mouseX, mouseY);
		}
	}

	private static void drawProceduralSymbol(GuiGraphicsExtractor graphics, int x, int y,
			int fill, HudEnergyMode mode) {
		int colour = switch (mode) {
			case NORMAL -> 0xFF33CC66;
			case EMPTY -> 0xFF777777;
			case DAMPENED -> 0xFFAA55CC;
			case DARKNESS -> 0xFF552277;
			case PROJECTION -> 0xFF55CCEE;
		};
		graphics.outline(x, y, SYMBOL_SIZE, SYMBOL_SIZE, 0xFFFFFFFF);
		if (fill > 0) graphics.fill(x + 2, y + 2, x + (fill == 1 ? 5 : 7), y + 7, colour);
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

}
