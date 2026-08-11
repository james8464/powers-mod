package com.powers.client.screen;

import com.powers.network.GrimoirePackets;
import com.powers.spell.SpellIndexEntry;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** Non-pausing compact contents page for selecting and understanding one grimoire's rituals. */
public final class GrimoireIndexScreen extends Screen {
	private static final int PANEL_WIDTH = 344;
	private static final int PANEL_HEIGHT = 224;
	private final String grimoireKey;
	private final List<SpellIndexEntry> entries;
	private final List<Button> rows = new ArrayList<>();
	private int preview;

	public GrimoireIndexScreen(String grimoireKey, int selected, List<SpellIndexEntry> entries) {
		super(Component.translatable("screen.powers.grimoire.title"));
		this.grimoireKey = grimoireKey;
		this.entries = List.copyOf(entries);
		this.preview = Math.clamp(selected, 0, Math.max(0, entries.size() - 1));
	}

	@Override
	protected void init() {
		rows.clear();
		int left = panelX();
		int top = panelY();
		for (int index = 0; index < entries.size(); index++) {
			int row = index;
			Button button = Button.builder(Component.translatable("spell.powers." + entries.get(index).id()),
					ignored -> {
						preview = row;
						refreshRows();
					}).bounds(left + 14, top + 38 + index * 28, 142, 22).build();
			rows.add(addRenderableWidget(button));
		}
		addRenderableWidget(Button.builder(Component.translatable("screen.powers.grimoire.select"),
				ignored -> select()).bounds(left + 182, top + 184, 136, 20).build());
		refreshRows();
	}

	private void refreshRows() {
		for (int index = 0; index < rows.size(); index++) rows.get(index).active = index != preview;
	}

	private void select() {
		ClientPlayNetworking.send(new GrimoirePackets.SelectSpellPayload(grimoireKey, preview));
		onClose();
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractBackground(graphics, mouseX, mouseY, delta);
		int left = panelX();
		int top = panelY();
		graphics.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, 0xF0D8C99D);
		graphics.outline(left, top, PANEL_WIDTH, PANEL_HEIGHT, 0xFF4B2F27);
		graphics.fill(left + 166, top + 26, left + 168, top + 210, 0x884B2F27);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);
		int left = panelX();
		int top = panelY();
		graphics.centeredText(font, title, width / 2, top + 12, 0xFF3A211C);
		if (entries.isEmpty()) return;
		SpellIndexEntry entry = entries.get(preview);
		int x = left + 180;
		graphics.text(font, Component.translatable("spell.powers." + entry.id()), x, top + 38,
				0xFF311B17, false);
		int y = drawWrapped(graphics, Component.translatable(entry.purposeKey()), x, top + 56, 146, 0xFF4B2F27);
		y = drawWrapped(graphics, Component.translatable("screen.powers.grimoire.target",
				Component.translatable(entry.targetKey()), compact(entry.range())), x, y + 5, 146, 0xFF5C362B);
		y = drawWrapped(graphics, Component.translatable("screen.powers.grimoire.cost",
				entry.energy(), ticks(entry.channelTicks()), ticks(entry.cooldownTicks())), x, y + 5, 146, 0xFF5C362B);
		drawWrapped(graphics, Component.translatable("screen.powers.grimoire.counter",
				Component.translatable(entry.counterKey())), x, y + 5, 146, 0xFF6A2730);
	}

	private int drawWrapped(GuiGraphicsExtractor graphics, Component text, int x, int y,
			int width, int color) {
		for (var line : font.split(text, width)) {
			graphics.text(font, line, x, y, color, false);
			y += 10;
		}
		return y;
	}

	private static String compact(double value) {
		return value == Math.rint(value) ? Integer.toString((int) value) : String.format(java.util.Locale.ROOT, "%.1f", value);
	}

	private static String ticks(int ticks) {
		if (ticks == 0) return "instant";
		return String.format(java.util.Locale.ROOT, "%.1fs", ticks / 20.0);
	}

	private int panelX() {
		return (width - PANEL_WIDTH) / 2;
	}

	private int panelY() {
		return Math.max(8, (height - PANEL_HEIGHT) / 2);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
