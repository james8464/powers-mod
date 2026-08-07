package com.powers.client.screen;

import com.powers.network.PowersPackets;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.screens.Screen;

import java.util.List;
import java.util.UUID;

/** the celestial grimoire's target picker: choose an online player to scry */
public class CelestialLocatorScreen extends Screen {
	private PlayerList playerList;

	public CelestialLocatorScreen() {
		super(Component.translatable("screen.powers.locator.title"));
	}

	@Override
	protected void init() {
		int cx = this.width / 2;

		// every online player you can see, minus yourself
		List<PlayerInfo> players = Minecraft.getInstance().getConnection() == null
				? List.of()
				: Minecraft.getInstance().getConnection().getOnlinePlayers().stream()
						.filter(info -> !info.getProfile().id().equals(Minecraft.getInstance().player.getUUID()))
						.toList();

		// one row per soul the stars can hunt
		playerList = new PlayerList(Minecraft.getInstance(), Math.min(200, this.width - 40), 180,
				40, 20);
		playerList.setX(cx - playerList.getWidth() / 2);
		for (PlayerInfo info : players) {
			playerList.add(new PlayerEntry(info.getProfile().id(), info.getProfile().name()));
		}
		this.addRenderableWidget(playerList);

		this.addRenderableWidget(Button.builder(Component.translatable("screen.powers.locator.cancel"),
				btn -> this.onClose()).bounds(cx - 60, 240, 120, 20).build());
	}

	private void choose(UUID targetUuid) {
		ClientPlayNetworking.send(new PowersPackets.LocatePlayerPayload(targetUuid));
		this.onClose();
	}

	private class PlayerList extends ObjectSelectionList<PlayerEntry> {
		private PlayerList(Minecraft minecraft, int width, int height, int y, int itemHeight) {
			super(minecraft, width, height, y, itemHeight);
		}

		// addEntry is protected; the screen hands rows in through this
		private void add(PlayerEntry entry) {
			addEntry(entry);
		}

		@Override
		protected void extractListBackground(GuiGraphicsExtractor g) {
			// translucent black panel behind the rows so names stay readable
			g.fill(getX() - 2, getY() - 2, getX() + getWidth() + 2, getY() + getHeight() + 2, 0x99000000);
		}
	}

	private class PlayerEntry extends ObjectSelectionList.Entry<PlayerEntry> {
		private final UUID id;
		private final String name;

		private PlayerEntry(UUID id, String name) {
			this.id = id;
			this.name = name;
		}

		@Override
		public void extractContent(GuiGraphicsExtractor g, int index, int y, boolean hovered, float delta) {
			int color = hovered ? 0xFFFFFFFF : 0xFFCFD8DC;
			int x = getContentXMiddle() - font.width(name) / 2;
			g.text(font, name, x, y + 6, color, false);
		}

		@Override
		public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
			if (event.button() == 0 && isMouseOver(event.x(), event.y())) {
				choose(id);
				return true;
			}
			return false;
		}

		@Override
		public Component getNarration() {
			return Component.literal(name);
		}
	}
}
