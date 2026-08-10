package com.powers.item.artifact;

import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/** Client/server-neutral filtering, paging, and responsive catalogue geometry. */
public final class ArtifactCatalogueRules {
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

	public static Layout layout(int screenWidth, int screenHeight) {
		int panelWidth = Math.max(1, Math.min(720, screenWidth - 16));
		int panelHeight = Math.max(1, Math.min(520, screenHeight - 16));
		int columns = panelWidth >= 560 ? 2 : 1;
		int rows = Math.max(1, (panelHeight - 112) / 24);
		return new Layout((screenWidth - panelWidth) / 2, (screenHeight - panelHeight) / 2,
				panelWidth, panelHeight, columns, rows);
	}

	public static int pageCount(int actionCount, int pageSize) {
		return Math.max(1, (Math.max(0, actionCount) + Math.max(1, pageSize) - 1)
				/ Math.max(1, pageSize));
	}

	public static <T> List<T> page(List<T> values, int page, int pageSize) {
		if (values == null || values.isEmpty()) return List.of();
		int safeSize = Math.max(1, pageSize);
		int safePage = Math.clamp(page, 0, pageCount(values.size(), safeSize) - 1);
		int from = safePage * safeSize;
		return values.subList(from, Math.min(values.size(), from + safeSize));
	}

	private static String normalize(String value) {
		return value == null ? "" : value.toLowerCase(Locale.ROOT)
				.replaceAll("[^a-z0-9]+", " ").trim();
	}

	public record Layout(int panelX, int panelY, int panelWidth, int panelHeight,
			int columns, int rows) {
		public int pageSize() {
			return columns * rows;
		}
	}
}
