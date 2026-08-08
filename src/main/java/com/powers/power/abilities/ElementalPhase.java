package com.powers.power.abilities;

/** Stable persisted phases and canonical action identities for Elemental Blast. */
public enum ElementalPhase {
	FLAME(0, "fireball", 0xFF5A24),
	FROST(1, "frost_nova", 0x82E9FF),
	STORM(2, "lightning_strike", 0xFFF59D),
	EARTH(3, "ground_slam", 0x8C66FF);

	private static final ElementalPhase[] VALUES = values();
	private final int index;
	private final String actionId;
	private final int color;

	ElementalPhase(int index, String actionId, int color) {
		this.index = index;
		this.actionId = actionId;
		this.color = color;
	}

	/** Returns the stable attachment index. */
	public int index() {
		return index;
	}

	/** Returns the existing canonical action executed by this phase. */
	public String actionId() {
		return actionId;
	}

	/** Returns the phase's authored 24-bit RGB identity. */
	public int color() {
		return color;
	}

	/** Normalizes arbitrary persisted data into a valid cyclic phase. */
	public static ElementalPhase fromIndex(int index) {
		return VALUES[Math.floorMod(index, VALUES.length)];
	}

	/** Returns the normalized index one phase ahead. */
	public static int nextIndex(int index) {
		return (fromIndex(index).index + 1) % VALUES.length;
	}

	/** Returns the normalized index one phase behind. */
	public static int previousIndex(int index) {
		return Math.floorMod(fromIndex(index).index - 1, VALUES.length);
	}
}
