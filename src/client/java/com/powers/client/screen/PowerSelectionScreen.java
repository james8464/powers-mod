package com.powers.client.screen;

import com.powers.client.ClientPowerState;
import com.powers.network.PowersPackets;
import com.powers.power.PassiveEffect;
import com.powers.power.Power;
import com.powers.power.PowerRegistry;
import com.powers.power.PowerEnergy;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class PowerSelectionScreen extends Screen {
	private static final int ENTRY_H = 22;
	private static final int LEFT_W = 170;
	private static final int RIGHT_W = 140;
	private static final int GAP = 6;
	private static final int ICON = 10;

	private final Set<String> selected = new LinkedHashSet<>();
	private final List<Power> powers;
	private String hoveredId;
	private int scrollOffset;
	private int maxVisible;
	private Component error;

	public PowerSelectionScreen() {
		super(Component.translatable("screen.powers.select"));
		this.powers = PowerRegistry.getAll();
		for (String id : ClientPowerState.getPowerIds()) {
			selected.add(id);
		}
	}

	@Override
	protected void init() {
		this.hoveredId = null;
		this.scrollOffset = 0;

		int totalW = LEFT_W + GAP + RIGHT_W;
		int leftX = (this.width - totalW) / 2;
		int listY = 32;
		int listH = Math.max(ENTRY_H, Math.min(Math.max(ENTRY_H, this.height - 70), powers.size() * ENTRY_H));
		this.maxVisible = Math.max(1, listH / ENTRY_H);

		int by = listY + listH + 4;
		int bw = (totalW - 6) / 2;

		this.addRenderableWidget(Button.builder(Component.translatable("screen.powers.confirm"), btn -> confirm())
				.bounds(leftX, by, bw, 20).build());
		this.addRenderableWidget(Button.builder(Component.translatable("screen.powers.randomize"), btn -> randomize())
				.bounds(leftX + bw + 6, by, bw, 20).build());
	}

	private void confirm() {
		if (selected.size() != 3) {
			error = Component.translatable("screen.powers.need_three");
			return;
		}
		ClientPlayNetworking.send(new PowersPackets.SetPowerSlotsPayload(new ArrayList<>(selected)));
		this.onClose();
	}

	private void randomize() {
		ClientPlayNetworking.send(new PowersPackets.RerollPowerSlotsPayload(0));
		this.onClose();
	}

	private void toggle(Power power) {
		String id = power.id().toString();
		if (selected.contains(id)) {
			selected.remove(id);
		} else if (selected.size() < 3) {
			selected.add(id);
		}
	}

	@Override
	public boolean mouseScrolled(double mx, double my, double horiz, double vert) {
		int totalW = LEFT_W + GAP + RIGHT_W;
		int leftX = (this.width - totalW) / 2;
		int listY = 32;
		if (mx >= leftX && mx <= leftX + LEFT_W && my >= listY && my <= listY + maxVisible * ENTRY_H) {
			int maxOffset = Math.max(0, powers.size() - maxVisible);
			scrollOffset = (int) Math.clamp(scrollOffset - vert, 0, maxOffset);
			return true;
		}
		return super.mouseScrolled(mx, my, horiz, vert);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() != 0) return super.mouseClicked(event, doubleClick);
		double mx = event.x();
		double my = event.y();

		int totalW = LEFT_W + GAP + RIGHT_W;
		int leftX = (this.width - totalW) / 2;
		int listY = 32;

		if (mx >= leftX && mx <= leftX + LEFT_W && my >= listY && my <= listY + maxVisible * ENTRY_H) {
			int idx = (int) ((my - listY) / ENTRY_H) + scrollOffset;
			if (idx >= 0 && idx < powers.size()) {
				toggle(powers.get(idx));
				return true;
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public void mouseMoved(double mx, double my) {
		int totalW = LEFT_W + GAP + RIGHT_W;
		int leftX = (this.width - totalW) / 2;
		int listY = 32;

		if (mx >= leftX && mx <= leftX + LEFT_W && my >= listY && my <= listY + maxVisible * ENTRY_H) {
			int idx = (int) ((my - listY) / ENTRY_H) + scrollOffset;
			if (idx >= 0 && idx < powers.size()) {
				hoveredId = powers.get(idx).id().toString();
				super.mouseMoved(mx, my);
				return;
			}
		}
		hoveredId = null;
		super.mouseMoved(mx, my);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		super.extractRenderState(g, mouseX, mouseY, delta);

		int totalW = LEFT_W + GAP + RIGHT_W;
		int leftX = (this.width - totalW) / 2;
		int rightX = leftX + LEFT_W + GAP;
		int listY = 32;
		int listH = maxVisible * ENTRY_H;

		g.text(this.font, this.title.getString(),
				(this.width - this.font.width(this.title)) / 2, 14, 0xFFFFFFFF, true);

		g.fill(leftX, listY, leftX + LEFT_W, listY + listH, 0x40000000);
		g.fill(rightX, listY, rightX + RIGHT_W, listY + listH, 0x40000000);

		for (int i = scrollOffset; i < Math.min(powers.size(), scrollOffset + maxVisible); i++) {
			Power p = powers.get(i);
			int y = listY + (i - scrollOffset) * ENTRY_H;
			boolean on = selected.contains(p.id().toString());
			boolean hover = p.id().toString().equals(hoveredId);

			if (hover) {
				g.fill(leftX + 1, y, leftX + LEFT_W - 1, y + ENTRY_H, 0x40FFFFFF);
			}
			if (on) {
				g.fill(leftX + 1, y, leftX + LEFT_W - 1, y + ENTRY_H, 0x3000FF00);
			}

			g.fill(leftX + 6, y + 6, leftX + 6 + ICON, y + 6 + ICON, 0xFF000000 | p.color());

			String name = truncate(p.name().getString(), LEFT_W - ICON - 28);
			g.text(this.font, name, leftX + ICON + 14, y + 4, 0xFFFFFFFF, true);

			String mark = on ? "[X]" : "[ ]";
			int mw = this.font.width(mark);
			g.text(this.font, mark, leftX + LEFT_W - mw - 8, y + 4, on ? 0xFF55FF55 : 0xFF666666, true);
		}

		g.fill(leftX, listY, leftX + LEFT_W, listY + 1, 0x44FFFFFF);
		g.fill(leftX, listY + listH - 1, leftX + LEFT_W, listY + listH, 0x44FFFFFF);
		g.fill(leftX, listY, leftX + 1, listY + listH, 0x44FFFFFF);
		g.fill(leftX + LEFT_W - 1, listY, leftX + LEFT_W, listY + listH, 0x44FFFFFF);

		if (powers.size() > maxVisible) {
			float thumbH = (float) maxVisible / powers.size() * listH;
			float thumbY = listY + (float) scrollOffset / powers.size() * listH;
			g.fill(leftX + LEFT_W - 4, (int) thumbY, leftX + LEFT_W - 2, (int) (thumbY + thumbH), 0x88FFFFFF);
		}

		renderDetail(g, rightX, listY, RIGHT_W, listH);

		int selTextX = leftX + totalW / 2;
		int selY = listY + listH + GAP + 24;
		String counter = "Selected: " + selected.size() + " / 3";
		g.text(this.font, counter, selTextX - this.font.width(counter) / 2, selY,
				selected.size() == 3 ? 0xFF55FF55 : 0xFFCCCCCC, true);
		if (error != null) {
			g.text(this.font, error.getString(),
					(this.width - this.font.width(error)) / 2, this.height - 18, 0xFFFF5555, true);
		}
	}

	private void renderDetail(GuiGraphicsExtractor g, int x, int y, int w, int h) {
		Power p = null;
		if (hoveredId != null) p = PowerRegistry.get(hoveredId);
		if (p == null) {
			String msg = "Hover a power\nto see details";
			int lw = this.font.width(msg.split("\n")[0]);
			g.text(this.font, msg, x + (w - lw) / 2, y + 30, 0xFF666666, true);
			return;
		}

		int py = y + 6;

		g.fill(x + 6, py, x + 6 + ICON, py + ICON, 0xFF000000 | p.color());
		String name = p.name().getString();
		int nameColor = p.color() | 0xFF000000;
		g.text(this.font, name, x + ICON + 14, py, nameColor, true);
		py += 14;

		g.textWithWordWrap(this.font, Component.literal(p.description().getString()),
				x + 6, py, w - 12, 0xFFCCCCCC);
		String desc = p.description().getString();
		int descLines = (int) Math.ceil((double) desc.length() * this.font.width("a") / (w - 12));
		py += descLines * 10 + 6;

		if (p.ability() != null && p.ability().isToggle()) {
			g.text(this.font, "Toggle", x + 6, py, 0xFF55FFFF, true);
		}
		py += 12;
		if (p.ability() != null) {
			g.text(this.font, "Energy: " + PowerEnergy.cost(p.ability()), x + 6, py, 0xFFB8FFF5, true);
			py += 12;
		}

		for (PassiveEffect pe : p.passives()) {
			String eff = "+ " + pe.effect().value().getDisplayName().getString();
			g.text(this.font, eff, x + 10, py, 0xFF88FF88, true);
			py += 10;
		}
	}

	private String truncate(String text, int maxWidth) {
		if (this.font.width(text) <= maxWidth) return text;
		String value = text;
		while (value.length() > 1 && this.font.width(value + "..") > maxWidth) {
			value = value.substring(0, value.length() - 1);
		}
		return value + "..";
	}
}
