package com.powers.item.artifact;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/** Client/server-neutral filtering and fixed-window responsive catalogue geometry. */
public final class ArtifactCatalogueRules {
	/** Retains the released column-major mapping: fill a column before advancing right. */
	public static int columnForSlot(int slot, int rows) {
		return Math.max(0, slot) / Math.max(1, rows);
	}

	public static int rowForSlot(int slot, int rows) {
		return Math.max(0, slot) % Math.max(1, rows);
	}
	private ArtifactCatalogueRules() {
	}

	public static List<ArtifactActionDefinition> filter(List<ArtifactActionDefinition> actions,
			ArtifactActionCategory category, String query,
			Function<ArtifactActionDefinition, String> labelProvider) {
		if (actions == null || labelProvider == null) return List.of();
		String needle = normalize(query);
		return actions.stream()
				.filter(action -> category == null || action.category() == category)
				.filter(action -> needle.isEmpty()
						|| normalize(action.key()).contains(needle)
						|| normalize(action.abilityId()).contains(needle)
						|| normalize(labelProvider.apply(action)).contains(needle))
				.toList();
	}

	/** Filters the four player-facing tabs, preserving the player's order for favourites. */
	public static List<ArtifactActionDefinition> filter(List<ArtifactActionDefinition> actions,
			ArtifactCatalogueTab tab, List<String> favourites, String query,
			Function<ArtifactActionDefinition, String> labelProvider) {
		return filter(actions, tab, favourites, List.of(), query, labelProvider);
	}

	/** Filters all player-facing tabs, preserving authored order for favourites and recents. */
	public static List<ArtifactActionDefinition> filter(List<ArtifactActionDefinition> actions,
			ArtifactCatalogueTab tab, List<String> favourites, List<String> recents, String query,
			Function<ArtifactActionDefinition, String> labelProvider) {
		if (actions == null || tab == null || labelProvider == null) return List.of();
		List<String> safeFavourites = favourites == null ? List.of() : favourites;
		List<String> safeRecents = recents == null ? List.of() : recents;
		String needle = normalize(query);
		List<ArtifactActionDefinition> filtered = actions.stream()
				.filter(action -> !needle.isEmpty() || switch (tab) {
					case FAVOURITES -> safeFavourites.contains(action.key());
					case RECENT -> safeRecents.contains(action.key());
					case INNATE -> action.category() == ArtifactActionCategory.ROUTED_POWER;
					case CRYSTALS -> action.category() == ArtifactActionCategory.ROUTED_CRYSTAL;
					case SWORD -> action.category() == ArtifactActionCategory.DOMINION;
				})
				.filter(action -> needle.isEmpty()
						|| normalize(action.key()).contains(needle)
						|| normalize(action.abilityId()).contains(needle)
						|| normalize(labelProvider.apply(action)).contains(needle))
				.toList();
		List<String> ordered = !needle.isEmpty() ? List.of()
				: tab == ArtifactCatalogueTab.FAVOURITES ? safeFavourites
				: tab == ArtifactCatalogueTab.RECENT ? safeRecents : List.of();
		return ordered.isEmpty() ? filtered : filtered.stream()
				.sorted(Comparator.comparingInt(action -> ordered.indexOf(action.key()))).toList();
	}

	public static Layout layout(int screenWidth, int screenHeight) {
		int panelWidth = Math.max(1, Math.min(720, screenWidth - 16));
		int panelHeight = Math.max(1, Math.min(520, screenHeight - 16));
		int columns = panelWidth >= 560 ? 2 : 1;
		int rows = Math.max(1, (panelHeight - 120) / 24);
		return new Layout((screenWidth - panelWidth) / 2, (screenHeight - panelHeight) / 2,
				panelWidth, panelHeight, columns, rows);
	}

	private static String normalize(String value) {
		return value == null ? "" : value.toLowerCase(Locale.ROOT)
				.replaceAll("[^a-z0-9]+", " ").trim();
	}

	public record Layout(int panelX, int panelY, int panelWidth, int panelHeight,
			int columns, int rows) {
		public int visibleCapacity() {
			return columns * rows;
		}
	}
}
