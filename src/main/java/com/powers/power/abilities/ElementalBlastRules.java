package com.powers.power.abilities;

/** Pure state transition policy for explicitly selected Elemental Blast phases. */
public final class ElementalBlastRules {
	private ElementalBlastRules() {
	}

	/** Casting never cycles the selection; it only normalizes malformed saved data. */
	public static int phaseAfterCast(int selectedPhase) {
		return ElementalPhase.fromIndex(selectedPhase).index();
	}
}
