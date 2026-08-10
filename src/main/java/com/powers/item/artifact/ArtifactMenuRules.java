package com.powers.item.artifact;

import java.util.List;

/** Pure grouping, paging, and malformed-packet fallback rules for the menu. */
public final class ArtifactMenuRules {
	public static final int PAGE_SIZE = 6;

	private ArtifactMenuRules() {
	}

	public static List<ArtifactActionDefinition> group(List<ArtifactActionDefinition> actions,
			ArtifactActionCategory category) {
		if (actions == null || category == null) return List.of();
		return actions.stream().filter(action -> action.category() == category).toList();
	}

	public static int pageCount(List<?> actions) {
		return Math.max(1, (Math.max(0, actions == null ? 0 : actions.size()) + PAGE_SIZE - 1)
				/ PAGE_SIZE);
	}

	public static <T> List<T> page(List<T> actions, int page) {
		if (actions == null || actions.isEmpty()) return List.of();
		int safePage = Math.clamp(page, 0, pageCount(actions) - 1);
		int from = safePage * PAGE_SIZE;
		return actions.subList(from, Math.min(actions.size(), from + PAGE_SIZE));
	}

	public static <T> T valueAt(List<T> values, int index, T fallback) {
		return values != null && index >= 0 && index < values.size() ? values.get(index) : fallback;
	}
}
