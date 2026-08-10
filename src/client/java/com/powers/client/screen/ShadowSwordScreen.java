package com.powers.client.screen;

import com.powers.item.artifact.ArtifactActionCatalogue;
import com.powers.item.artifact.ArtifactActionCategory;
import com.powers.item.artifact.ArtifactActionDefinition;
import com.powers.item.artifact.ArtifactAlignment;
import com.powers.item.artifact.ArtifactMenuRules;
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

/** Grouped vanilla-widget litany with server-derived cost and recovery state. */
public final class ShadowSwordScreen extends Screen {
	private final ArtifactAlignment alignment;
	private final String initialKey;
	private final int rank;
	private final int energy;
	private final List<Integer> costs;
	private final List<Integer> cooldowns;
	private final List<Integer> cooldownMaximums;
	private final List<Boolean> activeToggles;
	private int elementalPhase;
	private int sizeMorphOption;
	private final List<ArtifactActionDefinition> actions;
	private ArtifactActionDefinition selected;
	private ArtifactActionCategory category;
	private int page;

	public ShadowSwordScreen(String alignment, String initialKey, int rank,
			int elementalPhase, int sizeMorphOption, int energy,
			List<Integer> costs, List<Integer> cooldowns,
			List<Integer> cooldownMaximums, List<Boolean> activeToggles) {
		super(Component.translatable("screen.powers.artifact." + alignment));
		this.alignment = ArtifactAlignment.fromSerialized(alignment);
		this.initialKey = initialKey;
		this.rank = Math.clamp(rank, 0, 10);
		this.energy = Math.max(0, energy);
		this.actions = ArtifactActionCatalogue.forAlignment(this.alignment);
		this.costs = List.copyOf(costs);
		this.cooldowns = List.copyOf(cooldowns);
		this.cooldownMaximums = List.copyOf(cooldownMaximums);
		this.activeToggles = List.copyOf(activeToggles);
		this.elementalPhase = ElementalPhase.fromIndex(elementalPhase).index();
		this.sizeMorphOption = SizeMorphRules.isValidOption(sizeMorphOption)
				? sizeMorphOption : SizeMorphRules.normalOption();
	}

	@Override
	protected void init() {
		if (selected == null) {
			selected = actions.stream().filter(action -> action.key().equals(initialKey))
					.findFirst().orElse(actions.getFirst());
			category = selected.category();
		}
		int panelWidth = Math.min(420, width - 24);
		int left = (width - panelWidth) / 2;
		int top = Math.max(25, height / 2 - 95);
		int categoryWidth = (panelWidth - 8) / 3;
		for (int index = 0; index < ArtifactActionCategory.values().length; index++) {
			ArtifactActionCategory value = ArtifactActionCategory.values()[index];
			Button button = addRenderableWidget(Button.builder(sourceName(value), ignored -> {
				category = value;
				page = 0;
				rebuildWidgets();
			}).bounds(left + index * (categoryWidth + 4), top, categoryWidth, 20).build());
			button.active = value != category;
		}

		List<ArtifactActionDefinition> group = ArtifactMenuRules.group(actions, category);
		page = Math.clamp(page, 0, ArtifactMenuRules.pageCount(group) - 1);
		List<ArtifactActionDefinition> visible = ArtifactMenuRules.page(group, page);
		int rowsTop = top + 27;
		for (int row = 0; row < visible.size(); row++) {
			ArtifactActionDefinition action = visible.get(row);
			Button button = addRenderableWidget(Button.builder(rowLabel(action), ignored -> {
				selected = action;
				rebuildWidgets();
			}).bounds(left, rowsTop + row * 22, panelWidth, 20).build());
			button.active = rank >= action.requiredRank();
			button.setTooltip(Tooltip.create(actionTooltip(action)));
		}

		int footerY = rowsTop + ArtifactMenuRules.PAGE_SIZE * 22 + 1;
		Button previous = addRenderableWidget(Button.builder(Component.literal("◀"), ignored -> {
			page--;
			rebuildWidgets();
		}).bounds(left, footerY, 42, 20).build());
		previous.active = page > 0;
		Button next = addRenderableWidget(Button.builder(Component.literal("▶"), ignored -> {
			page++;
			rebuildWidgets();
		}).bounds(left + panelWidth - 42, footerY, 42, 20).build());
		next.active = page + 1 < ArtifactMenuRules.pageCount(group);

		if (selected.abilityId().equals("elemental_blast")) {
			addElementalSelector(left, panelWidth, footerY);
		} else if (selected.abilityId().equals("size_shift")) {
			addSizeSelector(left, panelWidth, footerY);
		}
		Button bind = addRenderableWidget(Button.builder(
				Component.translatable("screen.powers.shadow_sword.select"), ignored -> choose())
				.bounds(width / 2 - 70, footerY + 23, 140, 20).build());
		bind.active = rank >= selected.requiredRank();
	}

	private void addElementalSelector(int left, int panelWidth, int y) {
		addRenderableWidget(CycleButton.<Integer>builder(option -> Component.translatable(
				"hud.powers.element." + ElementalPhase.fromIndex(option).name()
						.toLowerCase(java.util.Locale.ROOT)), () -> elementalPhase)
				.withValues(IntStream.range(0, ElementalPhase.values().length).boxed().toList())
				.displayOnlyValue().create(left + 48, y, panelWidth - 96, 20,
						Component.translatable("screen.powers.shadow_sword.variant"),
						(button, option) -> elementalPhase = option));
	}

	private void addSizeSelector(int left, int panelWidth, int y) {
		addRenderableWidget(CycleButton.<Integer>builder(
				option -> Component.literal(SizeMorphRules.scale(option) + "×"), () -> sizeMorphOption)
				.withValues(IntStream.range(0, SizeMorphRules.scales().size()).boxed().toList())
				.displayOnlyValue().create(left + 48, y, panelWidth - 96, 20,
						Component.translatable("screen.powers.shadow_sword.variant"),
						(button, option) -> sizeMorphOption = option));
	}

	private Component rowLabel(ArtifactActionDefinition action) {
		int index = actions.indexOf(action);
		int cost = ArtifactMenuRules.valueAt(costs, index, action.energyCost());
		int remaining = ArtifactMenuRules.valueAt(cooldowns, index, 0);
		boolean active = ArtifactMenuRules.valueAt(activeToggles, index, false);
		Component state = rank < action.requiredRank()
				? Component.translatable("screen.powers.artifact.row.locked", action.requiredRank())
				: active ? Component.translatable("screen.powers.artifact.row.active")
				: remaining > 0 ? Component.translatable("screen.powers.artifact.row.cooldown",
						(remaining + 19) / 20)
				: Component.translatable("screen.powers.artifact.row.ready");
		String marker = selected == action ? "▶ " : "  ";
		return Component.literal(marker + categoryGlyph(action.category()) + " ")
				.append(actionName(action)).append(Component.literal("  "))
				.append(Component.translatable("screen.powers.artifact.row.cost", cost))
				.append(Component.literal("  ")).append(state);
	}

	private Component actionTooltip(ArtifactActionDefinition action) {
		int index = actions.indexOf(action);
		int cost = ArtifactMenuRules.valueAt(costs, index, action.energyCost());
		int remaining = ArtifactMenuRules.valueAt(cooldowns, index, 0);
		int maximum = ArtifactMenuRules.valueAt(cooldownMaximums, index,
				action.baseCooldownTicks());
		Component description = Component.translatableWithFallback(
				descriptionKey(action), humanize(action.abilityId()) + " invocation");
		return Component.empty().append(sourceName(action.category())).append("\n")
				.append(description).append("\n")
				.append(Component.translatable("screen.powers.artifact.tooltip.live",
						cost, energy, action.requiredRank(), remaining / 20.0,
						maximum / 20.0));
	}

	private Component actionName(ArtifactActionDefinition action) {
		String key = action.category() == ArtifactActionCategory.ROUTED_POWER
				? "power.powers." + action.abilityId() : "ability.powers." + action.abilityId();
		Component name = Component.translatableWithFallback(key, humanize(action.abilityId()));
		return name.copy().withStyle(rank < action.requiredRank() ? ChatFormatting.DARK_GRAY
				: action.category() == ArtifactActionCategory.DOMINION
						? alignment == ArtifactAlignment.DARKNESS
								? ChatFormatting.DARK_PURPLE : ChatFormatting.GOLD
						: ChatFormatting.GRAY);
	}

	private static String descriptionKey(ArtifactActionDefinition action) {
		return (action.category() == ArtifactActionCategory.ROUTED_POWER
				? "power.powers." : "ability.powers.") + action.abilityId() + ".description";
	}

	private void choose() {
		if (selected == null || rank < selected.requiredRank()) return;
		int option = selected.abilityId().equals("elemental_blast") ? elementalPhase
				: selected.abilityId().equals("size_shift") ? sizeMorphOption : -1;
		ClientPlayNetworking.send(new ShadowSwordPackets.SelectPayload(
				alignment.serializedName(), selected.key(), option));
		onClose();
	}

	private static Component sourceName(ArtifactActionCategory source) {
		return Component.translatable("screen.powers.artifact.source."
				+ source.name().toLowerCase(java.util.Locale.ROOT));
	}

	private static String categoryGlyph(ArtifactActionCategory source) {
		return switch (source) {
			case ROUTED_POWER -> "◆";
			case ROUTED_CRYSTAL -> "◇";
			case DOMINION -> "✦";
		};
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
		int panelWidth = Math.min(440, width - 12);
		int panelLeft = (width - panelWidth) / 2;
		graphics.fill(panelLeft, 6, panelLeft + panelWidth, height - 5, 0xD9120E18);
		graphics.fill(panelLeft, 6, panelLeft + panelWidth, 9,
				alignment == ArtifactAlignment.DARKNESS ? 0xFF4A2754 : 0xFFFFD76A);
		super.extractRenderState(graphics, mouseX, mouseY, delta);
		graphics.centeredText(font, title, width / 2, 10,
				alignment == ArtifactAlignment.DARKNESS ? 0xFFC89AD4 : 0xFFFFE7A0);
		graphics.centeredText(font, Component.translatable("screen.powers.artifact.summary",
				rank, energy), width / 2, 20, 0xFFB8B0C0);
	}
}
