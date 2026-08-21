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
	private final ArtifactMenuState state;
	private List<String> favourites;
	private int hovered = ArtifactWheelRules.NONE;
	private int openTicks;

	public ShadowSwordScreen(long revision, String alignment, String initialKey, int rank,
			int sizeMorphOption, int energy,
			List<String> favourites,
			List<com.powers.item.artifact.ArtifactActionSnapshot> snapshots) {
		this(revision, alignment, initialKey, rank, sizeMorphOption, energy, favourites,
				List.of(), snapshots);
	}

	public ShadowSwordScreen(long revision, String alignment, String initialKey, int rank,
			int sizeMorphOption, int energy, List<String> favourites, List<String> recents,
			List<com.powers.item.artifact.ArtifactActionSnapshot> snapshots) {
		this(ArtifactMenuState.fromPacket(revision, alignment, initialKey, rank,
				sizeMorphOption, energy, favourites, recents, snapshots));
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
		ArtifactWheelRules.Layout layout = ArtifactWheelRules.layout(width, height);
		int target = ArtifactWheelRules.targetAt(width / 2, wheelCenterY(), event.x(), event.y(),
				layout.outerRadius());
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
				? new ShadowSwordPackets.CommitPayload(state.revision(), state.alignment().serializedName(),
						action.key(), state.optionFor(action))
				: new ShadowSwordPackets.SelectPayload(state.revision(), state.alignment().serializedName(),
						action.key(), state.optionFor(action)));
		onClose();
	}

	private void openCatalogue() {
		minecraft.gui.setScreen(new ArtifactCatalogueScreen(state));
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		int centerX = width / 2;
		int centerY = wheelCenterY();
		ArtifactWheelRules.Layout layout = ArtifactWheelRules.layout(width, height);
		hovered = ArtifactWheelRules.targetAt(centerX, centerY, mouseX, mouseY,
				layout.outerRadius());
		drawWheel(graphics, centerX, centerY, layout);
		int titleY = Math.max(8, centerY - layout.outerRadius() - 19);
		graphics.centeredText(font, title, centerX, titleY, accent());
		Component secondary = Component.translatable(ClientInteractionPreferences.releaseToCast()
				? "screen.powers.artifact.wheel.hint_cast" : "screen.powers.artifact.wheel.hint");
		if (hovered >= 0) {
			ArtifactActionDefinition action = state.action(favourites.get(hovered));
			if (action != null) secondary = state.actionName(action);
		}
		graphics.centeredText(font, secondary, centerX, titleY + 13, 0xFFCAC4D3);
	}

	private void drawWheel(GuiGraphicsExtractor graphics, int centerX, int centerY,
			ArtifactWheelRules.Layout layout) {
		for (int slot = 0; slot < ArtifactWheelRules.SLOT_COUNT; slot++) {
			double angle = -Math.PI / 2.0 + slot * Math.PI * 2.0 / ArtifactWheelRules.SLOT_COUNT;
			int glyphX = centerX + (int) Math.round(Math.cos(angle) * layout.glyphRadius());
			int glyphY = centerY + (int) Math.round(Math.sin(angle) * layout.verticalGlyphRadius());
			drawSegment(graphics, glyphX, glyphY, slot == hovered);
			ArtifactActionDefinition action = state.action(favourites.get(slot));
			int color = action == null || state.locked(action)
					? 0xFF66616D : action.key().equals(state.selectedKey()) ? 0xFFFFFFFF : accent();
			AbilityGlyphRenderer.draw(graphics, action == null ? null : action.abilityId(),
					glyphX, glyphY, color);
			graphics.text(font, Integer.toString(slot + 1), glyphX - 14, glyphY - 13,
					slot == hovered ? 0xFFFFFFFF : 0xFFAAA3B2, true);
			if (action != null) drawLiveStatus(graphics, action, glyphX, glyphY, layout);
		}
		int centerColor = hovered == ArtifactWheelRules.CENTER ? 0xE06E547E : 0xD01A1621;
		graphics.fill(centerX - 31, centerY - 11, centerX + 31, centerY + 11, centerColor);
		graphics.fill(centerX - 28, centerY - 8, centerX + 28, centerY + 8, 0xEE0D0B11);
		graphics.centeredText(font, Component.translatable("screen.powers.artifact.wheel.catalogue"),
				centerX, centerY - 4, hovered == ArtifactWheelRules.CENTER ? 0xFFFFFFFF : accent());
	}

	private void drawLiveStatus(GuiGraphicsExtractor graphics, ArtifactActionDefinition action,
			int glyphX, int glyphY, ArtifactWheelRules.Layout layout) {
		ArtifactWheelRules.SegmentStatus status = ArtifactWheelRules.segmentStatus(
				state.cost(action), state.energy(),
				ArtifactWheelRules.remainingCooldown(state.cooldown(action), openTicks),
				state.cooldownMaximum(action),
				state.active(action), state.locked(action), state.variant(action));
		String fullName = state.actionName(action).getString();
		String name = font.width(fullName) <= layout.nameWidth() ? fullName
				: ArtifactWheelRules.compactLabel(fullName, Math.max(4, layout.nameWidth() / 6));
		graphics.centeredText(font, name, glyphX, glyphY + 9,
				status.locked() ? 0xFF77727C : 0xFFE8E2EC);
		int costY = glyphY + 18;
		String variant = switch (action.abilityId()) {
			case "size_shift" -> status.variant() >= 0 && SizeMorphRules.isValidOption(status.variant())
					? " " + SizeMorphRules.scale(status.variant()) + "×" : "";
			case "gravity_displacement" -> status.variant() >= 0
					? " " + Component.translatable(switch (com.powers.item.artifact.ArtifactMenuRules
							.normalizeGravityOption(status.variant())) {
						case 0 -> "ability.powers.gravity_displacement.mode.pull";
						case 1 -> "ability.powers.gravity_displacement.mode.orbit";
						default -> "ability.powers.gravity_displacement.mode.repel";
					}).getString() : "";
			default -> "";
		};
		graphics.centeredText(font, status.cost() + "E" + variant, glyphX, costY,
				status.locked() ? 0xFF77727C
						: status.energySufficient() ? 0xFFBDB5C7 : 0xFFFF7777);
		int barLeft = glyphX - 12;
		int barY = costY + 9;
		graphics.fill(barLeft, barY, barLeft + 24, barY + 2, 0xB0201A26);
		if (status.cooldownPips() > 0) {
			graphics.fill(barLeft, barY, barLeft + status.cooldownPips() * 3,
					barY + 2, 0xFFE08A6A);
		}
		if (status.active()) {
			graphics.text(font, "◆", glyphX + 10, glyphY - 14, 0xFF7CFFB2, true);
		}
		if (status.locked()) {
			graphics.text(font, "×", glyphX - 3, glyphY - 4, 0xFFFF7777, true);
		}
	}

	private void drawSegment(GuiGraphicsExtractor graphics, int glyphX, int glyphY,
			boolean highlighted) {
		int fill = highlighted ? 0xE06E547E : 0xC02A2231;
		graphics.fill(glyphX - 12, glyphY - 12, glyphX + 12, glyphY + 12, fill);
		graphics.fill(glyphX - 10, glyphY - 10, glyphX + 10, glyphY + 10, 0xD00D0B11);
	}

	private int wheelCenterY() {
		return height / 2 - Math.min(25, height / 12);
	}

	private int accent() {
		return state.alignment() == ArtifactAlignment.DARKNESS ? 0xFFC89AD4 : 0xFFFFE7A0;
	}
}
