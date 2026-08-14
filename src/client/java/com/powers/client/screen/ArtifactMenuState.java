package com.powers.client.screen;

import com.powers.cooldown.CooldownPresentation;
import com.powers.item.artifact.ArtifactActionCatalogue;
import com.powers.item.artifact.ArtifactActionCategory;
import com.powers.item.artifact.ArtifactActionDefinition;
import com.powers.item.artifact.ArtifactActionSnapshot;
import com.powers.item.artifact.ArtifactAlignment;
import com.powers.power.abilities.SizeMorphRules;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Locale;

/** Immutable server-authored live data shared by the quick wheel and full catalogue. */
public record ArtifactMenuState(
		long revision,
		ArtifactAlignment alignment,
		String selectedKey,
		int rank,
		int sizeMorphOption,
		int energy,
		List<String> favourites,
		List<ArtifactActionDefinition> actions,
		List<ArtifactActionSnapshot> snapshots) {
	public ArtifactMenuState {
		rank = Math.clamp(rank, 0, 10);
		energy = Math.max(0, energy);
		favourites = List.copyOf(favourites);
		actions = List.copyOf(actions);
		snapshots = List.copyOf(snapshots);
		sizeMorphOption = SizeMorphRules.isValidOption(sizeMorphOption)
				? sizeMorphOption : SizeMorphRules.normalOption();
	}

	public static ArtifactMenuState fromPacket(long revision, String alignment, String selectedKey, int rank,
			int sizeMorphOption, int energy, List<String> favourites,
			List<ArtifactActionSnapshot> snapshots) {
		ArtifactAlignment parsed = ArtifactAlignment.fromSerialized(alignment);
		return new ArtifactMenuState(revision, parsed, selectedKey, rank, sizeMorphOption,
				energy, favourites, ArtifactActionCatalogue.forAlignment(parsed), snapshots);
	}

	public ArtifactMenuState withFavourites(List<String> updated) {
		return new ArtifactMenuState(revision, alignment, selectedKey, rank, sizeMorphOption,
				energy, updated, actions, snapshots);
	}

	public ArtifactActionDefinition action(String key) {
		return ArtifactActionCatalogue.find(alignment, key);
	}

	public int cost(ArtifactActionDefinition action) {
		ArtifactActionSnapshot snapshot = snapshot(action);
		return snapshot == null ? action.energyCost() : snapshot.cost();
	}

	public int cooldown(ArtifactActionDefinition action) {
		ArtifactActionSnapshot snapshot = snapshot(action);
		return snapshot == null ? 0 : snapshot.cooldownTicks();
	}

	public int energySaved(ArtifactActionDefinition action) {
		ArtifactActionSnapshot snapshot = snapshot(action);
		return snapshot == null ? 0 : snapshot.energySaved();
	}

	public int cooldownMaximum(ArtifactActionDefinition action) {
		ArtifactActionSnapshot snapshot = snapshot(action);
		return snapshot == null ? action.baseCooldownTicks() : snapshot.cooldownMaximumTicks();
	}

	public boolean active(ArtifactActionDefinition action) {
		ArtifactActionSnapshot snapshot = snapshot(action);
		return snapshot != null && snapshot.active();
	}

	public boolean locked(ArtifactActionDefinition action) {
		ArtifactActionSnapshot snapshot = snapshot(action);
		return snapshot == null ? rank < action.requiredRank() : snapshot.locked();
	}

	public int variant(ArtifactActionDefinition action) {
		ArtifactActionSnapshot snapshot = snapshot(action);
		return snapshot == null ? -1 : snapshot.variant();
	}

	public int optionFor(ArtifactActionDefinition action) {
		return switch (action.abilityId()) {
			case "size_shift" -> sizeMorphOption;
			case "gravity_displacement" -> com.powers.item.artifact.ArtifactMenuRules
					.normalizeGravityOption(variant(action));
			default -> -1;
		};
	}

	public Component actionName(ArtifactActionDefinition action) {
		String key = action.category() == ArtifactActionCategory.ROUTED_POWER
				? "power.powers." + action.abilityId() : "ability.powers." + action.abilityId();
		return Component.translatableWithFallback(key, humanize(action.abilityId())).copy()
				.withStyle(locked(action) ? ChatFormatting.DARK_GRAY
						: action.category() == ArtifactActionCategory.DOMINION
								? alignment == ArtifactAlignment.DARKNESS
										? ChatFormatting.DARK_PURPLE : ChatFormatting.GOLD
								: ChatFormatting.GRAY);
	}

	public Component tooltip(ArtifactActionDefinition action) {
		String descriptionKey = (action.category() == ArtifactActionCategory.ROUTED_POWER
				? "power.powers." : "ability.powers.") + action.abilityId() + ".description";
		Component description = Component.translatableWithFallback(descriptionKey,
				humanize(action.abilityId()) + " invocation");
		Component live = Component.translatable("screen.powers.artifact.tooltip.live",
				cost(action), energy, action.requiredRank(), CooldownPresentation.tenths(cooldown(action)),
				CooldownPresentation.tenths(cooldownMaximum(action)));
		if (energySaved(action) > 0) live = live.copy().append("\n").append(
				Component.translatable("screen.powers.artifact.tooltip.malignember",
						energySaved(action)));
		return Component.empty().append(sourceName(action.category())).append("\n")
				.append(description).append("\n")
				.append(live);
	}

	public static Component sourceName(ArtifactActionCategory source) {
		return Component.translatable("screen.powers.artifact.source."
				+ source.name().toLowerCase(Locale.ROOT));
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

	private ArtifactActionSnapshot snapshot(ArtifactActionDefinition action) {
		return snapshots.stream().filter(value -> value.key().equals(action.key())).findFirst().orElse(null);
	}
}
