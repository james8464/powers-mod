package com.powers.client;

import com.powers.companion.ShadowHudRules;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/** Contextual owner-only Shadow status rendered only while active or rebuilding. */
final class ShadowStatusHudRenderer {
	private ShadowStatusHudRenderer() {
	}

	static void render(GuiGraphicsExtractor graphics) {
		Minecraft client = Minecraft.getInstance();
		var status = PrivateCompanionClient.ownerStatus();
		boolean owner = client.player != null && status != null
				&& status.ownerId().equals(client.player.getUUID());
		if (status == null || !ShadowHudRules.visible(owner,
				status.active(), status.recallTicks() > 0)) return;
		int x = graphics.guiWidth() - 126;
		int y = 9;
		graphics.fill(x, y, x + 117, y + 29, 0xB0100C16);
		graphics.outline(x, y, 117, 29, 0xFF8B5AA0);
		if (status.active()) {
			int width = Math.round(107.0F * status.energy() / Math.max(1, status.maximumEnergy()));
			graphics.text(client.font, Component.translatable("hud.powers.shadow.status",
					Component.translatable("hud.powers.shadow.stance." + status.stance()),
					status.energy(), status.maximumEnergy()), x + 5, y + 5,
					0xFFE2C7EA, false);
			graphics.fill(x + 5, y + 18, x + 112, y + 22, 0xFF241A2A);
			graphics.fill(x + 5, y + 18, x + 5 + width, y + 22,
					status.suppressed() ? 0xFFB35D72 : 0xFF8D58AE);
		} else {
			graphics.text(client.font, Component.translatable("hud.powers.shadow.recall",
					(int) Math.ceil(status.recallTicks() / 20.0)), x + 5, y + 10,
					0xFFE2C7EA, false);
		}
	}
}
