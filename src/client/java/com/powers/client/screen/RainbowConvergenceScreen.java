package com.powers.client.screen;

import com.powers.client.AbilityGlyphRenderer;
import com.powers.network.CrystalSelectorPackets;
import com.powers.power.crystals.CrystalSelectorRules;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Combat-safe six-force Rainbow selector using the artifact glyph language. */
public final class RainbowConvergenceScreen extends Screen {
	private final List<String> modes;
	private final int selected;

	public RainbowConvergenceScreen(List<String> modes, int selected) {
		super(Component.translatable("screen.powers.crystal_selector.title"));
		this.modes = List.copyOf(modes);
		this.selected = Math.clamp(selected, 0, Math.max(0, modes.size() - 1));
	}

	@Override
	protected void init() {
		CrystalSelectorRules.Layout layout = CrystalSelectorRules.layout(width, height);
		for (int slot = 0; slot < modes.size(); slot++) {
			double angle = -Math.PI / 2.0 + slot * Math.PI * 2.0 / modes.size();
			int x = layout.centerX() + (int) Math.round(Math.cos(angle)
					* layout.horizontalRadius()) - layout.buttonWidth() / 2;
			int y = layout.centerY() + (int) Math.round(Math.sin(angle)
					* layout.verticalRadius()) - 10;
			int target = slot;
			String abilityId = modes.get(slot);
			String translation = abilityId.equals("middleworld")
					? "ability.powers.middleworld.short" : "ability.powers." + abilityId;
			Component label = Component.literal((slot + 1) + ". ").append(
					Component.translatableWithFallback(translation, abilityId));
			addRenderableWidget(Button.builder(label,
					ignored -> choose(target)).bounds(x, y, layout.buttonWidth(), 20).build());
		}
	}

	@Override public boolean isPauseScreen() { return false; }

	@Override
	public boolean keyPressed(KeyEvent event) {
		int slot = CrystalSelectorRules.numberSlot(event.key(), modes.size());
		if (slot >= 0) {
			choose(slot);
			return true;
		}
		return super.keyPressed(event);
	}

	private void choose(int slot) {
		ClientPlayNetworking.send(new CrystalSelectorPackets.SelectPayload(slot));
		onClose();
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics,
			int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);
		CrystalSelectorRules.Layout layout = CrystalSelectorRules.layout(width, height);
		graphics.centeredText(font, title, layout.centerX(), layout.titleY(), 0xFFFFD9FF);
		for (int slot = 0; slot < modes.size(); slot++) {
			double angle = -Math.PI / 2.0 + slot * Math.PI * 2.0 / modes.size();
			int x = layout.centerX() + (int) Math.round(Math.cos(angle)
					* layout.horizontalRadius() * 0.52);
			int y = layout.centerY() + (int) Math.round(Math.sin(angle)
					* layout.verticalRadius() * 0.52);
			AbilityGlyphRenderer.draw(graphics, modes.get(slot), x, y,
					slot == selected ? 0xFFFFFFFF : 0xFFFF9CEB);
		}
		graphics.centeredText(font, Component.translatable("screen.powers.crystal_selector.hint"),
				layout.centerX(), layout.hintY(), 0xFFE0C6E8);
	}
}
