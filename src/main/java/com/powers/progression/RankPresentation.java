package com.powers.progression;

/** Human-readable descriptions of the mechanics that maze branches actually unlock. */
public final class RankPresentation {
	private static final float MINIMUM_READABLE_SCALE = 0.55F;

	private RankPresentation() {
	}

	/** Returns truthful compact UI copy without exposing retired percentage perks. */
	public static String summary(RankNode node) {
		if (node.canonical()) {
			return "Innate tier " + node.depth()
					+ " · authored power strength, reach, duration, destruction and capacity";
		}
		return switch (node.branch()) {
			case "might" -> "Empowered Impact · boss-scale offensive transformation";
			case "motion" -> "Second Step · kinetic movement transformation";
			case "insight" -> "True Sight · reveals concealed magical traces";
			case "wardcraft" -> "Reflective Ward · forcefield reflection transformation";
			case "communion" -> "Soul Echo · stronger soul transfer and echoes";
			case "veil" -> "Afterimage · quieter residue and hostile forgetting";
			case "dominion" -> "Ancient Mastery · capstone power transformation";
			case "abyss" -> "Dark Resurgence · Darkness amplification at low energy";
			default -> "Connected title · unlocks a new route through the Labyrinth";
		};
	}

	/** Fits one label inside its lane while retaining a legible lower bound. */
	public static float readableScale(int availableWidth, int measuredWidth) {
		if (availableWidth <= 0 || measuredWidth <= 0) return 1.0F;
		return Math.clamp(availableWidth / (float) measuredWidth,
				MINIMUM_READABLE_SCALE, 1.0F);
	}
}
