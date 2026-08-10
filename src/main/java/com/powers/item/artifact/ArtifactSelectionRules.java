package com.powers.item.artifact;

/** Pure validation for stable action IDs arriving from artifact menus. */
public final class ArtifactSelectionRules {
	private ArtifactSelectionRules() {
	}

	public static boolean maySelect(ArtifactActionDefinition action,
			ArtifactAlignment heldAlignment, int rank) {
		return action != null && action.alignment() == heldAlignment && rank >= action.requiredRank();
	}
}
