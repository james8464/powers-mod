package com.powers.companion;

/** Sanitized fictional state shared by deterministic and optional text providers. */
public record LoreDialogueContext(String realm, boolean lowHealth, boolean lowEnergy,
		int rank, String nearbyAlignment, String artifactAction,
		boolean recentDeath, boolean bossNearby, String milestone) {
	public LoreDialogueContext {
		realm = safe(realm, 40);
		nearbyAlignment = safe(nearbyAlignment, 24);
		artifactAction = safe(artifactAction, 48);
		milestone = safe(milestone, 48);
		rank = Math.clamp(rank, 0, 10);
	}

	public static LoreDialogueContext calm(String realm, int rank) {
		return new LoreDialogueContext(realm, false, false, rank, "none", "none",
				false, false, "none");
	}

	private static String safe(String value, int maximum) {
		if (value == null) return "none";
		String cleaned = value.replaceAll("[^a-zA-Z0-9_ -]", "").strip();
		return cleaned.isEmpty() ? "none" : cleaned.substring(0, Math.min(maximum, cleaned.length()));
	}
}
