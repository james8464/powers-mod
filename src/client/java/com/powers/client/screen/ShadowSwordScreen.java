package com.powers.client.screen;

import com.powers.item.ShadowSwordCatalogue;
import com.powers.network.ShadowSwordPackets;
import com.powers.power.abilities.ElementalPhase;
import com.powers.power.abilities.SizeMorphRules;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.stream.IntStream;

/** Vanilla-styled selector for the sword's large, rank-gated action catalogue. */
public final class ShadowSwordScreen extends Screen {
	private final String initialKey;
	private final int darknessLevel;
	private int elementalPhase;
	private int sizeMorphOption;
	private final List<ShadowSwordCatalogue.Definition> actions = ShadowSwordCatalogue.definitions();
	private ShadowSwordCatalogue.Definition selected;
	private CycleButton<ShadowSwordCatalogue.Definition> actionButton;
	private CycleButton<Integer> elementalButton;
	private CycleButton<Integer> sizeButton;

	public ShadowSwordScreen(String initialKey, int darknessLevel, int elementalPhase, int sizeMorphOption) {
		super(Component.translatable("screen.powers.shadow_sword"));
		this.initialKey = initialKey;
		this.darknessLevel = darknessLevel;
		this.elementalPhase = ElementalPhase.fromIndex(elementalPhase).index();
		this.sizeMorphOption = SizeMorphRules.isValidOption(sizeMorphOption)
				? sizeMorphOption : SizeMorphRules.normalOption();
	}

	@Override
	protected void init() {
		selected = actions.stream().filter(action -> action.key().equals(initialKey))
				.findFirst().orElse(actions.getFirst());
		actionButton = addRenderableWidget(CycleButton.<ShadowSwordCatalogue.Definition>builder(
				this::label, () -> selected).withValues(actions).displayOnlyValue()
				.create(width / 2 - 130, height / 2 - 20, 260, 20,
						Component.translatable("screen.powers.shadow_sword.power"),
						(button, action) -> {
							selected = action;
							updateOptionVisibility();
							updateTooltip();
						}));
		elementalButton = addRenderableWidget(CycleButton.<Integer>builder(
				option -> Component.translatable("hud.powers.element."
						+ ElementalPhase.fromIndex(option).name().toLowerCase(java.util.Locale.ROOT)),
				() -> this.elementalPhase)
				.withValues(IntStream.range(0, ElementalPhase.values().length).boxed().toList())
				.displayOnlyValue().create(width / 2 - 80, height / 2 + 8, 160, 20,
						Component.translatable("screen.powers.shadow_sword.variant"),
						(button, option) -> this.elementalPhase = option));
		sizeButton = addRenderableWidget(CycleButton.<Integer>builder(
				option -> Component.literal(SizeMorphRules.scale(option) + "×"), () -> this.sizeMorphOption)
				.withValues(IntStream.range(0, SizeMorphRules.scales().size()).boxed().toList())
				.displayOnlyValue().create(width / 2 - 80, height / 2 + 8, 160, 20,
						Component.translatable("screen.powers.shadow_sword.variant"),
						(button, option) -> this.sizeMorphOption = option));
		addRenderableWidget(Button.builder(Component.translatable("screen.powers.shadow_sword.select"),
				button -> choose()).bounds(width / 2 - 60, height / 2 + 38, 120, 20).build());
		updateOptionVisibility();
		updateTooltip();
	}

	private void updateOptionVisibility() {
		if (elementalButton == null || sizeButton == null || selected == null) return;
		elementalButton.visible = selected.abilityId().equals("elemental_blast");
		elementalButton.active = elementalButton.visible;
		sizeButton.visible = selected.abilityId().equals("size_shift");
		sizeButton.active = sizeButton.visible;
	}

	private Component label(ShadowSwordCatalogue.Definition action) {
		String key = action.source() == ShadowSwordCatalogue.Source.INNATE
				? "power.powers." + action.abilityId() : "ability.powers." + action.abilityId();
		Component name = Component.translatableWithFallback(key, humanize(action.abilityId()));
		if (darknessLevel < action.requiredDarknessRank()) {
			return Component.translatable("screen.powers.shadow_sword.locked_label",
					action.requiredDarknessRank(), name).withStyle(ChatFormatting.DARK_GRAY);
		}
		return name.copy().withStyle(action.source() == ShadowSwordCatalogue.Source.DARKNESS
				? ChatFormatting.DARK_PURPLE : ChatFormatting.GRAY);
	}

	private void updateTooltip() {
		if (actionButton == null || selected == null) return;
		actionButton.setTooltip(Tooltip.create(Component.translatable(
				"screen.powers.shadow_sword.tooltip", sourceName(selected.source()),
				selected.requiredDarknessRank())));
	}

	private void choose() {
		if (selected != null && darknessLevel >= selected.requiredDarknessRank()) {
			int option = selected.abilityId().equals("elemental_blast") ? elementalPhase
					: selected.abilityId().equals("size_shift") ? sizeMorphOption : -1;
			ClientPlayNetworking.send(new ShadowSwordPackets.SelectPayload(selected.key(), option));
			onClose();
		}
	}

	private static Component sourceName(ShadowSwordCatalogue.Source source) {
		return Component.translatable("screen.powers.shadow_sword.source."
				+ source.name().toLowerCase(java.util.Locale.ROOT));
	}

	private static String humanize(String value) {
		String[] words = value.split("_");
		StringBuilder result = new StringBuilder();
		for (String word : words) {
			if (!result.isEmpty()) result.append(' ');
			result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
		}
		return result.toString();
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);
		graphics.centeredText(font, title, width / 2, height / 2 - 58, 0xFF4A3B50);
		graphics.centeredText(font, Component.translatable("screen.powers.shadow_sword.rank", darknessLevel),
				width / 2, height / 2 - 43, 0xFF777777);
	}
}
