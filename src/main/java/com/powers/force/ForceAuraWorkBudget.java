package com.powers.force;

/** Per-level hard budget shared by player-centred living-force aura queries. */
public final class ForceAuraWorkBudget {
	private final int globalLimit;
	private final int perPlayerLimit;
	private int inspected;

	public ForceAuraWorkBudget(int globalLimit, int perPlayerLimit) {
		this.globalLimit = Math.max(0, globalLimit);
		this.perPlayerLimit = Math.max(0, perPlayerLimit);
	}

	public boolean hasWork() {
		return inspected < globalLimit;
	}

	public int allowanceForPlayer() {
		return Math.min(perPlayerLimit, Math.max(0, globalLimit - inspected));
	}

	public void recordInspections(int count) {
		if (count <= 0) return;
		inspected = Math.min(globalLimit, inspected + count);
	}

	public int inspected() {
		return inspected;
	}
}
