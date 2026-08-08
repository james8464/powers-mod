package com.powers.client.screen;

import com.powers.PowersMod;
import com.powers.network.PowersPackets;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.UUID;

/** Texture-backed celestial target picker with responsive, bounded layout. */
public final class CelestialLocatorScreen extends Screen {
	private static final Identifier PANEL = PowersMod.id("textures/gui/locator_panel.png");
	private static final int PANEL_WIDTH = 240;
	private static final int PANEL_HEIGHT = 224;

	private final UUID nonce;
	private PlayerList playerList;
	private boolean empty;

	public CelestialLocatorScreen(UUID nonce) {
		super(Component.translatable("screen.powers.locator.title"));
		this.nonce = nonce;
	}

	@Override
	protected void init() {
		int left = panelX();
		int top = panelY();
		List<PlayerInfo> players = Minecraft.getInstance().getConnection() == null
				? List.of()
				: Minecraft.getInstance().getConnection().getOnlinePlayers().stream()
						.filter(info -> Minecraft.getInstance().player != null
								&& !info.getProfile().id().equals(Minecraft.getInstance().player.getUUID()))
						.sorted(java.util.Comparator.comparing(info -> info.getProfile().name()))
						.toList();
		empty = players.isEmpty();
		playerList = new PlayerList(Minecraft.getInstance(), 200, 134, top + 38, 24);
		playerList.setX(left + 20);
		for (PlayerInfo info : players) {
			playerList.add(new PlayerEntry(info.getProfile().id(), info.getProfile().name()));
		}
		addRenderableWidget(playerList);
		addRenderableWidget(Button.builder(Component.translatable("screen.powers.locator.cancel"),
				button -> onClose()).bounds(left + 60, top + 184, 120, 20).build());
	}

	private void choose(UUID targetUuid) {
		ClientPlayNetworking.send(new PowersPackets.LocatePlayerPayload(targetUuid, nonce));
		onClose();
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractBackground(graphics, mouseX, mouseY, delta);
		graphics.blit(RenderPipelines.GUI_TEXTURED, PANEL, panelX(), panelY(), 0, 0,
				PANEL_WIDTH, PANEL_HEIGHT, PANEL_WIDTH, PANEL_HEIGHT);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);
		graphics.centeredText(font, title, width / 2, panelY() + 15, 0xFFF1E8FF);
		if (empty) graphics.centeredText(font, Component.translatable("screen.powers.locator.empty"),
				width / 2, panelY() + 98, 0xFF9C91AF);
	}

	private int panelX() {
		return (width - PANEL_WIDTH) / 2;
	}

	private int panelY() {
		return Math.max(8, (height - PANEL_HEIGHT) / 2);
	}

	private final class PlayerList extends ObjectSelectionList<PlayerEntry> {
		private PlayerList(Minecraft minecraft, int width, int height, int y, int itemHeight) {
			super(minecraft, width, height, y, itemHeight);
		}

		private void add(PlayerEntry entry) {
			addEntry(entry);
		}

		@Override
		protected void extractListBackground(GuiGraphicsExtractor graphics) {
			graphics.fill(getX() - 2, getY() - 2, getX() + getWidth() + 2,
					getY() + getHeight() + 2, 0xB5070911);
			graphics.outline(getX() - 2, getY() - 2, getWidth() + 4, getHeight() + 4, 0xAA665C99);
		}
	}

	private final class PlayerEntry extends ObjectSelectionList.Entry<PlayerEntry> {
		private final UUID id;
		private final String name;

		private PlayerEntry(UUID id, String name) {
			this.id = id;
			this.name = name;
		}

		@Override
		public void extractContent(GuiGraphicsExtractor graphics, int index, int y,
				boolean hovered, float delta) {
			int center = getContentXMiddle();
			if (hovered) graphics.fill(center - 94, y + 1, center + 94, y + 23, 0x553E2C61);
			drawDiamond(graphics, center - 82, y + 12, 3, hovered ? 0xFFD6B7FF : 0xFF7455A8);
			graphics.text(font, name, center - 72, y + 8,
					hovered ? 0xFFFFFFFF : 0xFFD8D2E2, false);
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

	private static void drawDiamond(GuiGraphicsExtractor graphics, int centerX, int centerY,
			int radius, int color) {
		for (int dy = -radius; dy <= radius; dy++) {
			int half = radius - Math.abs(dy);
			graphics.fill(centerX - half, centerY + dy,
					centerX + half + 1, centerY + dy + 1, color);
		}
	}
}
