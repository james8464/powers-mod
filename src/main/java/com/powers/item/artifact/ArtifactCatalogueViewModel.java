package com.powers.item.artifact;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

/** Revision-aware virtual catalogue state independent of Minecraft widgets. */
public final class ArtifactCatalogueViewModel {
	private final int columns;
	private final int rows;
	private long revision;
	private List<ArtifactActionDefinition> actions = List.of();
	private Map<String, String> searchText = Map.of();
	private List<String> favourites = List.of();
	private List<String> recents = List.of();
	private List<ArtifactActionDefinition> filtered = List.of();
	private ArtifactCatalogueTab tab = ArtifactCatalogueTab.FAVOURITES;
	private String query = "";
	private String selectedKey;
	private int firstVisibleIndex;

	public ArtifactCatalogueViewModel(long revision, List<ArtifactActionDefinition> actions,
			List<String> favourites, List<String> recents, String selectedKey,
			Function<ArtifactActionDefinition, String> labelProvider, int columns, int rows) {
		this.columns = Math.max(1, columns);
		this.rows = Math.max(1, rows);
		refresh(revision, actions, favourites, recents, selectedKey, labelProvider);
	}

	/** Replaces one server-authored revision while preserving a still-canonical local selection. */
	public void refresh(long revision, List<ArtifactActionDefinition> actions,
			List<String> favourites, List<String> recents, String serverSelectedKey,
			Function<ArtifactActionDefinition, String> labelProvider) {
		this.revision = revision;
		this.actions = actions == null ? List.of() : List.copyOf(actions);
		this.favourites = favourites == null ? List.of() : List.copyOf(favourites);
		List<String> keys = this.actions.stream().map(ArtifactActionDefinition::key).toList();
		this.recents = ArtifactRecentRules.reconcile(recents, keys);
		Map<String, String> indexed = new HashMap<>(Math.max(16, this.actions.size() * 2));
		Function<ArtifactActionDefinition, String> labels = labelProvider == null
				? ArtifactActionDefinition::abilityId : labelProvider;
		for (ArtifactActionDefinition action : this.actions) {
			indexed.put(action.key(), normalize(action.key() + " " + action.abilityId() + " "
					+ labels.apply(action)));
		}
		searchText = Map.copyOf(indexed);
		if (find(selectedKey) == null) selectedKey = find(serverSelectedKey) == null
				? (this.actions.isEmpty() ? null : this.actions.getFirst().key()) : serverSelectedKey;
		refilter(false);
	}

	public void setFilter(ArtifactCatalogueTab tab, String query) {
		this.tab = tab == null ? ArtifactCatalogueTab.FAVOURITES : tab;
		this.query = normalize(query);
		refilter(true);
	}

	public void scrollRows(int deltaRows) {
		int maximumStart = Math.max(0, filtered.size() - poolSize());
		firstVisibleIndex = Math.clamp(firstVisibleIndex + deltaRows, 0, maximumStart);
	}

	public List<ArtifactActionDefinition> visible() {
		int end = Math.min(filtered.size(), firstVisibleIndex + poolSize());
		return filtered.subList(firstVisibleIndex, end);
	}

	public void selectVisible(int slot) {
		List<ArtifactActionDefinition> visible = visible();
		if (slot >= 0 && slot < visible.size()) selectedKey = visible.get(slot).key();
	}

	/** Moves focus through filtered results and keeps the chosen row inside the virtual window. */
	public void moveSelection(int delta) {
		if (filtered.isEmpty() || delta == 0) return;
		int current = -1;
		for (int index = 0; index < filtered.size(); index++) {
			if (filtered.get(index).key().equals(selectedKey)) {
				current = index;
				break;
			}
		}
		int next = Math.clamp((current < 0 ? 0 : current) + delta, 0, filtered.size() - 1);
		selectedKey = filtered.get(next).key();
		if (next < firstVisibleIndex) {
			firstVisibleIndex = next;
		} else if (next >= firstVisibleIndex + poolSize()) {
			firstVisibleIndex = next - poolSize() + 1;
		}
	}

	public BindingIntent bind(int favouriteSlot) {
		return favouriteSlot < 0 || favouriteSlot >= ArtifactFavouriteRules.SLOT_COUNT
				|| find(selectedKey) == null ? null : new BindingIntent(favouriteSlot, selectedKey);
	}

	public ArtifactActionDefinition selected() {
		return find(selectedKey);
	}

	public long revision() {
		return revision;
	}

	public int poolSize() {
		return columns * rows;
	}

	public int filteredCount() {
		return filtered.size();
	}

	public int firstVisibleIndex() {
		return firstVisibleIndex;
	}

	public String selectedKey() {
		return selectedKey;
	}

	public ArtifactCatalogueTab tab() {
		return tab;
	}

	public String query() {
		return query;
	}

	private void refilter(boolean resetScroll) {
		Map<String, Integer> favouriteOrder = order(favourites);
		Map<String, Integer> recentOrder = order(recents);
		List<ArtifactActionDefinition> result = new ArrayList<>();
		for (ArtifactActionDefinition action : actions) {
			boolean inTab = !query.isEmpty() || switch (tab) {
				case FAVOURITES -> favouriteOrder.containsKey(action.key());
				case RECENT -> recentOrder.containsKey(action.key());
				case INNATE -> action.category() == ArtifactActionCategory.ROUTED_POWER;
				case CRYSTALS -> action.category() == ArtifactActionCategory.ROUTED_CRYSTAL;
				case SWORD -> action.category() == ArtifactActionCategory.DOMINION;
			};
			if (inTab && (query.isEmpty() || searchText.getOrDefault(action.key(), "").contains(query))) {
				result.add(action);
			}
		}
		if (query.isEmpty() && tab == ArtifactCatalogueTab.FAVOURITES) {
			result.sort(java.util.Comparator.comparingInt(action -> favouriteOrder.get(action.key())));
		} else if (query.isEmpty() && tab == ArtifactCatalogueTab.RECENT) {
			result.sort(java.util.Comparator.comparingInt(action -> recentOrder.get(action.key())));
		}
		filtered = List.copyOf(result);
		if (resetScroll) firstVisibleIndex = 0;
		else scrollRows(0);
	}

	private ArtifactActionDefinition find(String key) {
		if (key == null) return null;
		for (ArtifactActionDefinition action : actions) {
			if (action.key().equals(key)) return action;
		}
		return null;
	}

	private static Map<String, Integer> order(List<String> keys) {
		Map<String, Integer> result = new HashMap<>();
		for (int index = 0; index < keys.size(); index++) result.putIfAbsent(keys.get(index), index);
		return result;
	}

	private static String normalize(String value) {
		return value == null ? "" : value.toLowerCase(Locale.ROOT)
				.replaceAll("[^a-z0-9]+", " ").trim();
	}

	public record BindingIntent(int slot, String actionKey) {
	}
}
