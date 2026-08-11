package com.powers.companion;

/** Pure relevance gate for the contextual owner-only Shadow HUD. */
public final class ShadowHudRules {
	private ShadowHudRules() {
	}

	public static boolean visible(boolean owner, boolean active, boolean recallCoolingDown) {
		return owner && (active || recallCoolingDown);
	}
}
