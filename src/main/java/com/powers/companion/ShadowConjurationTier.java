package com.powers.companion;

/** Server-authored conjuration costs; one invocation creates at most one plain stack. */
public enum ShadowConjurationTier {
	COMMON(4), UNCOMMON(12), RARE(40), MYTHIC(250);

	private final int energyCost;

	ShadowConjurationTier(int energyCost) {
		this.energyCost = energyCost;
	}

	public int energyCost() {
		return energyCost;
	}
}
