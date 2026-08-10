package com.powers.item.artifact;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Pure eight-slot persistent loadout defaults, migration, and editing rules. */
public final class ArtifactFavouriteRules {
	public static final int SLOT_COUNT = 8;
	private static final List<String> SHADOW_DEFAULTS = List.of(
			"innate/lightning_strike", "innate/fireball", "innate/time_shift",
			"innate/forcefield", "innate/flight", "unique/call_hollowed",
			"unique/blight_ground", "unique/nightfall_dominion");
	private static final List<String> LIGHT_DEFAULTS = List.of(
			"innate/lightning_strike", "innate/fireball", "innate/time_shift",
			"innate/forcefield", "innate/flight", "dominion/call_radiant",
			"dominion/consecrate_ground", "dominion/host_heaven");

	private ArtifactFavouriteRules() {
	}

	public static List<String> defaults(List<ArtifactActionDefinition> actions, int rank,
			String selectedKey) {
		ArtifactAlignment alignment = actions == null || actions.isEmpty()
				? ArtifactAlignment.DARKNESS : actions.getFirst().alignment();
		return reconcile(List.of(), actions, alignment, rank, selectedKey);
	}

	/** Migrates old keys, removes duplicates, and fills all missing combat slots. */
	public static List<String> reconcile(List<String> stored,
			List<ArtifactActionDefinition> actions, ArtifactAlignment alignment,
			int rank, String selectedKey) {
		List<ArtifactActionDefinition> safeActions = actions == null ? List.of() : actions;
		Set<String> available = safeActions.stream().map(ArtifactActionDefinition::key)
				.collect(java.util.stream.Collectors.toUnmodifiableSet());
		LinkedHashSet<String> result = new LinkedHashSet<>();
		if (stored != null) {
			for (String key : stored) {
				if (key == null || key.isBlank()) continue;
				String migrated = alignment == ArtifactAlignment.DARKNESS
						&& key.equals("unique/nightfall_dominion") ? key
						: ArtifactSelectionMigration.migrate(alignment, key, rank);
				if (available.contains(migrated)) result.add(migrated);
				if (result.size() >= SLOT_COUNT) break;
			}
		}
		List<String> defaults = alignment == ArtifactAlignment.DARKNESS
				? SHADOW_DEFAULTS : LIGHT_DEFAULTS;
		for (String key : defaults) {
			if (result.size() >= SLOT_COUNT) break;
			if (available.contains(key)) result.add(key);
		}
		for (ArtifactActionDefinition action : safeActions) {
			if (result.size() >= SLOT_COUNT) break;
			result.add(action.key());
		}
		List<String> padded = new ArrayList<>(result);
		while (padded.size() < SLOT_COUNT) padded.add("");
		return List.copyOf(padded);
	}

	public static List<String> assign(List<String> favourites, int slot, String key) {
		List<String> result = padded(favourites);
		if (slot < 0 || slot >= SLOT_COUNT || key == null || key.isBlank()) return List.copyOf(result);
		int existing = result.indexOf(key);
		String displaced = result.get(slot);
		result.set(slot, key);
		if (existing >= 0 && existing != slot) result.set(existing, displaced);
		return List.copyOf(result);
	}

	/** Wraps selection across the combat favourites without entering the catalogue. */
	public static String cycle(List<String> favourites, String current, int direction) {
		if (direction != -1 && direction != 1) return current;
		List<String> available = padded(favourites).stream().filter(key -> !key.isBlank()).toList();
		if (available.isEmpty()) return current;
		int index = available.indexOf(current);
		if (index < 0) return direction > 0 ? available.getFirst() : available.getLast();
		return available.get(Math.floorMod(index + direction, available.size()));
	}

	private static List<String> padded(List<String> favourites) {
		List<String> result = new ArrayList<>(SLOT_COUNT);
		if (favourites != null) {
			for (String key : favourites) {
				if (result.size() >= SLOT_COUNT) break;
				result.add(key == null ? "" : key);
			}
		}
		while (result.size() < SLOT_COUNT) result.add("");
		return result;
	}
}
