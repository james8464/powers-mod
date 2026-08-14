package com.powers.ai;

/** Explicit bounded scan shape for each AI perception responsibility. */
public enum PerceptionQueryProfile {
	GUARDIAN_FIELD(16),
	ALLY_LANE(16),
	SHADOW_TARGET(64),
	GUARDIAN_TARGET(256);

	private final int inspectionLimit;

	PerceptionQueryProfile(int inspectionLimit) {
		this.inspectionLimit = inspectionLimit;
	}

	public int inspectionLimit() {
		return inspectionLimit;
	}
}
