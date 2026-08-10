package com.powers.item.artifact;

import java.util.List;

/** Pure validation for stable action IDs arriving from artifact menus. */
public final class ArtifactSelectionRules {
	private ArtifactSelectionRules() {
	}

	public static boolean maySelect(ArtifactActionDefinition action,
			ArtifactAlignment heldAlignment, int rank) {
		return action != null && action.alignment() == heldAlignment && rank >= action.requiredRank();
	}

	/** Returns one wrapped, rank-eligible step; malformed current keys restart at an edge. */
	public static String cycleKey(List<ArtifactActionDefinition> actions, String currentKey,
			ArtifactAlignment heldAlignment, int rank, int direction) {
		if (actions == null || heldAlignment == null || Math.abs(direction) != 1) return null;
		List<ArtifactActionDefinition> eligible = actions.stream()
				.filter(action -> maySelect(action, heldAlignment, rank)).toList();
		if (eligible.isEmpty()) return null;
		int current = -1;
		for (int index = 0; index < eligible.size(); index++) {
			if (eligible.get(index).key().equals(currentKey)) {
				current = index;
				break;
			}
		}
		int next = current < 0 ? (direction > 0 ? 0 : eligible.size() - 1)
				: Math.floorMod(current + direction, eligible.size());
		return eligible.get(next).key();
	}
}
