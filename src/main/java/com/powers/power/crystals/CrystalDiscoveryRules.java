package com.powers.power.crystals;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.OptionalInt;

/** Pure canonicalization and legacy-seeding rules for crystal attunement discovery. */
public final class CrystalDiscoveryRules {
	private CrystalDiscoveryRules() {
	}

	public static List<String> normalize(List<String> modes, List<String> persisted,
			OptionalInt legacySelection) {
		if (modes == null || modes.isEmpty()) return List.of();
		var allowed = new LinkedHashSet<>(modes);
		var normalized = new LinkedHashSet<String>();
		if (persisted != null) {
			for (String discovered : persisted) {
				if (allowed.contains(discovered)) normalized.add(discovered);
			}
		}
		if (normalized.isEmpty() && legacySelection != null && legacySelection.isPresent()) {
			normalized.add(modes.get(Math.floorMod(legacySelection.getAsInt(), modes.size())));
		}
		return modes.stream().filter(normalized::contains).toList();
	}

	public static List<String> discover(List<String> modes, List<String> persisted, String actionId) {
		List<String> normalized = normalize(modes, persisted, OptionalInt.empty());
		if (actionId == null || !modes.contains(actionId) || normalized.contains(actionId)) {
			return normalized;
		}
		var updated = new LinkedHashSet<>(normalized);
		updated.add(actionId);
		return modes.stream().filter(updated::contains).toList();
	}

	public static boolean known(List<String> discovered, String actionId) {
		return discovered != null && discovered.contains(actionId);
	}
}
