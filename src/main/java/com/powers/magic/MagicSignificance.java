package com.powers.magic;

/**
 * Declares how much generic ceremony an action needs in addition to its
 * gameplay-specific presentation. Keeping this in the canonical catalogue
 * prevents every successful cast from receiving the same noisy particle show.
 */
public enum MagicSignificance {
	NONE(0),
	MINIMAL(1),
	STANDARD(2),
	RITUAL(4),
	COSMIC(6);

	private final int genericBeatCount;

	MagicSignificance(int genericBeatCount) {
		this.genericBeatCount = genericBeatCount;
	}

	/** Returns the authored number of locally generated ceremony beats. */
	public int genericBeatCount() {
		return genericBeatCount;
	}
}
