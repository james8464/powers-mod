package com.powers.player;

/**
 * Chooses the one progression tree that may be visible in the advancement
 * journal. Stored levels remain independent, so changing allegiance hides a
 * tree without erasing the rank the player previously earned there.
 */
public final class AdvancementPathRules {
	/** A complete active/hidden selection for one synchronisation pass. */
	public record Selection(String activeRoot, String hiddenRoot, int reachedLevel) {
	}

	private AdvancementPathRules() {
	}

	/** Returns the visible path and its stored completion floor. */
	public static Selection select(boolean darkness, int skillLevel, int darknessLevel) {
		return darkness
				? new Selection("darkness_root", "skill_root", darknessLevel)
				: new Selection("skill_root", "darkness_root", skillLevel);
	}

	/** Avoids repeating advancement mutation and network work on every player pulse. */
	public static boolean needsSynchronization(Selection previous, Selection current) {
		return current != null && !current.equals(previous);
	}
}
