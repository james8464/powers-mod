package com.powers.realm;

/** Pure bounded cursor used to split landmark construction across server ticks. */
public final class RealmConstructionCursor {
	private final int total;
	private int position;

	public RealmConstructionCursor(int total) {
		if (total < 0) throw new IllegalArgumentException("total must not be negative");
		this.total = total;
	}

	public int claim(int budget) {
		if (budget <= 0 || complete()) return 0;
		int claimed = Math.min(budget, total - position);
		position += claimed;
		return claimed;
	}

	public int position() {
		return position;
	}

	public boolean complete() {
		return position >= total;
	}
}
