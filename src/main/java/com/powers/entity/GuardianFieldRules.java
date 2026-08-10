package com.powers.entity;

import com.powers.item.artifact.ArtifactAlignment;

/** Pure faction and internal-cooldown policy for guardian alignment fields. */
public final class GuardianFieldRules {
	private GuardianFieldRules() {
	}

	public static boolean pulseAt(int tick, boolean elite) {
		int interval = elite ? 80 : 120;
		return Math.floorMod(tick, interval) == 0;
	}

	public static boolean hostile(ArtifactAlignment alignment, boolean targetDarkness) {
		return alignment == ArtifactAlignment.DARKNESS ? !targetDarkness : targetDarkness;
	}
}
