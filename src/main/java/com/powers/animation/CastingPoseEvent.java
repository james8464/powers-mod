package com.powers.animation;

import java.util.UUID;

/** Immutable, compact server-authored pose event. */
public record CastingPoseEvent(int entityId, UUID entityUuid, long sequence,
		CastingPose pose, CastingStyle style, CastingHand hand,
		long startGameTime, int durationTicks) {
	private static final UUID ZERO_UUID = new UUID(0L, 0L);

	public CastingPoseEvent {
		if (entityId < 0 || entityUuid == null || ZERO_UUID.equals(entityUuid)) {
			throw new IllegalArgumentException("entity identity");
		}
		if (sequence <= 0 || pose == null || style == null || hand == null) {
			throw new IllegalArgumentException("pose identity");
		}
		if (startGameTime < 0 || durationTicks < 1 || durationTicks > CastingPoseRules.MAX_DURATION_TICKS) {
			throw new IllegalArgumentException("pose timing");
		}
		try {
			Math.addExact(startGameTime, durationTicks);
		} catch (ArithmeticException overflow) {
			throw new IllegalArgumentException("pose timing overflow", overflow);
		}
	}

	public long endGameTime() {
		return startGameTime + durationTicks;
	}
}
