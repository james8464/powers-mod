package com.powers.client.screen;

import com.powers.client.AbilityGlyphRenderer;
import com.powers.client.ClientInteractionPreferences;
import com.powers.item.artifact.ArtifactActionDefinition;
import com.powers.item.artifact.ArtifactAlignment;
import com.powers.item.artifact.ArtifactWheelRules;
import com.powers.network.ShadowSwordPackets;
import com.powers.power.abilities.SizeMorphRules;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Non-pausing eight-favourite artifact wheel backed by server-authored menu state. */
public final class ShadowSwordScreen extends Screen {
	private static final int GLYPH_RADIUS = 55;
	private final ArtifactMenuState state;
	private List<String> favourites;
	private int hovered = ArtifactWheelRules.NONE;
	private int openTicks;

	public ShadowSwordScreen(String alignment, String initialKey, int rank,
			int sizeMorphOption, int energy,
			List<String> favourites,
			List<com.powers.item.artifact.ArtifactActionSnapshot> snapshots) {
		this(ArtifactMenuState.fromPacket(alignment, initialKey, rank,
				sizeMorphOption, energy, favourites, snapshots));
	}

	ShadowSwordScreen(ArtifactMenuState state) {
		super(Component.translatable("screen.powers.artifact.wheel.title"));
		this.state = state;
		this.favourites = state.favourites();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void tick() {
		openTicks++;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		// The wheel deliberately leaves gameplay visible and unpaused.
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() != 0) return super.mouseClicked(event, doubleClick);
		int target = ArtifactWheelRules.targetAt(width / 2, height / 2, event.x(), event.y());
		if (target == ArtifactWheelRules.CENTER) {
			openCatalogue();
			return true;
		}
		if (target >= 0) {
			choose(target);
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		int slot = ArtifactWheelRules.numberSlot(event.key());
		if (slot >= 0) {
			choose(slot);
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public boolean keyReleased(KeyEvent event) {
		ArtifactWheelRules.ReleaseAction release = ArtifactWheelRules.releaseAction(
				ClientInteractionPreferences.releaseToCast(), event.key(), hovered);
		if (release != ArtifactWheelRules.ReleaseAction.NONE) {
			choose(hovered, release == ArtifactWheelRules.ReleaseAction.CAST);
			return true;
		}
		return super.keyReleased(event);
	}

	private void choose(int slot) {
		choose(slot, false);
	}

	private void choose(int slot, boolean cast) {
		if (slot < 0 || slot >= favourites.size()) return;
		ArtifactActionDefinition action = state.action(favourites.get(slot));
		if (action == null || state.locked(action)) return;
		ClientPlayNetworking.send(cast
				? new ShadowSwordPackets.CommitPayload(state.alignment().serializedName(),
						action.key(), state.optionFor(action))
				: new ShadowSwordPackets.SelectPayload(state.alignment().serializedName(),
						action.key(), state.optionFor(action)));
		onClose();
	}

	private void openCatalogue() {
		minecraft.gui.setScreen(new ArtifactCatalogueScreen(state));
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		int centerX = width / 2;
		int centerY = height / 2;
		hovered = ArtifactWheelRules.targetAt(centerX, centerY, mouseX, mouseY);
		drawWheel(graphics, centerX, centerY);
		graphics.centeredText(font, title, centerX, centerY - 101, accent());
		graphics.centeredText(font, Component.translatable(ClientInteractionPreferences.releaseToCast()
				? "screen.powers.artifact.wheel.hint_cast" : "screen.powers.artifact.wheel.hint"),
				centerX, centerY + 92, 0xFFCAC4D3);
		if (hovered >= 0) {
			ArtifactActionDefinition action = state.action(favourites.get(hovered));
			if (action != null) {
				graphics.centeredText(font, state.actionName(action), centerX, centerY + 79, 0xFFFFFFFF);
			}
		}
	}

	private void drawWheel(GuiGraphicsExtractor graphics, int centerX, int centerY) {
		for (int slot = 0; slot < ArtifactWheelRules.SLOT_COUNT; slot++) {
			drawSegment(graphics, centerX, centerY, slot, slot == hovered);
			double angle = -Math.PI / 2.0 + slot * Math.PI * 2.0 / ArtifactWheelRules.SLOT_COUNT;
			int glyphX = centerX + (int) Math.round(Math.cos(angle) * GLYPH_RADIUS);
			int glyphY = centerY + (int) Math.round(Math.sin(angle) * GLYPH_RADIUS);
			ArtifactActionDefinition action = state.action(favourites.get(slot));
			int color = action == null || state.locked(action)
					? 0xFF66616D : action.key().equals(state.selectedKey()) ? 0xFFFFFFFF : accent();
			AbilityGlyphRenderer.draw(graphics, action == null ? null : action.abilityId(),
					glyphX, glyphY, color);
			graphics.text(font, Integer.toString(slot + 1), glyphX - 14, glyphY - 13,
					slot == hovered ? 0xFFFFFFFF : 0xFFAAA3B2, true);
			if (action != null) drawLiveStatus(graphics, action, glyphX, glyphY);
		}
		int centerColor = hovered == ArtifactWheelRules.CENTER ? 0xE04B3A58 : 0xD01A1621;
		AbilityGlyphRenderer.diamond(graphics, centerX, centerY, 22, centerColor);
		AbilityGlyphRenderer.diamond(graphics, centerX, centerY, 19, 0xEE0D0B11);
		graphics.centeredText(font, Component.translatable("screen.powers.artifact.wheel.catalogue"),
				centerX, centerY - 4, hovered == ArtifactWheelRules.CENTER ? 0xFFFFFFFF : accent());
	}

	private void drawLiveStatus(GuiGraphicsExtractor graphics, ArtifactActionDefinition action,
			int glyphX, int glyphY) {
		ArtifactWheelRules.SegmentStatus status = ArtifactWheelRules.segmentStatus(
				state.cost(action), state.energy(),
				ArtifactWheelRules.remainingCooldown(state.cooldown(action), openTicks),
				state.cooldownMaximum(action),
				state.active(action), state.locked(action), state.variant(action));
		String name = font.plainSubstrByWidth(state.actionName(action).getString(), 36);
		graphics.centeredText(font, name, glyphX, glyphY + 9,
				status.locked() ? 0xFF77727C : 0xFFE8E2EC);
		String variant = status.variant() >= 0 && SizeMorphRules.isValidOption(status.variant())
				? " " + SizeMorphRules.scale(status.variant()) + "×" : "";
		graphics.centeredText(font, status.cost() + "E" + variant, glyphX, glyphY + 18,
				status.locked() ? 0xFF77727C
						: status.energySufficient() ? 0xFFBDB5C7 : 0xFFFF7777);
		int barLeft = glyphX - 12;
		graphics.fill(barLeft, glyphY + 27, barLeft + 24, glyphY + 29, 0xB0201A26);
		if (status.cooldownPips() > 0) {
			graphics.fill(barLeft, glyphY + 27, barLeft + status.cooldownPips() * 3,
					glyphY + 29, 0xFFE08A6A);
		}
		if (status.active()) {
			graphics.text(font, "◆", glyphX + 10, glyphY - 14, 0xFF7CFFB2, true);
		}
		if (status.locked()) {
			graphics.text(font, "×", glyphX - 3, glyphY - 4, 0xFFFF7777, true);
		}
	}

	private void drawSegment(GuiGraphicsExtractor graphics, int centerX, int centerY,
			int slot, boolean highlighted) {
		double centerAngle = -Math.PI / 2.0 + slot * Math.PI * 2.0 / ArtifactWheelRules.SLOT_COUNT;
		int fill = highlighted ? 0xD06E547E : 0xB52A2231;
		for (int radial = 29; radial <= ArtifactWheelRules.OUTER_RADIUS; radial += 4) {
			for (int spoke = -3; spoke <= 3; spoke++) {
				double angle = centerAngle + spoke * Math.PI / 32.0;
				int x = centerX + (int) Math.round(Math.cos(angle) * radial);
				int y = centerY + (int) Math.round(Math.sin(angle) * radial);
				graphics.fill(x - 2, y - 2, x + 2, y + 2, fill);
			}
		}
	}

	private int accent() {
		return state.alignment() == ArtifactAlignment.DARKNESS ? 0xFFC89AD4 : 0xFFFFE7A0;
	}
}
