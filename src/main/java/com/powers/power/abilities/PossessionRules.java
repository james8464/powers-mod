package com.powers.power.abilities;

/** Pure target eligibility and consent classification for Vessel Possession. */
public final class PossessionRules {
	public static final int MAX_DURATION_TICKS = 600;
	/** Identifies which permission and travel policy owns a shared remote-control session. */
	public enum SessionKind {
		POSSESSION,
		DREAMWALK
	}
	/** Only real players and AI-controlled mobs can host a possession camera. */
	public enum TargetKind {
		PLAYER,
		MOB,
		OTHER
	}

	private PossessionRules() {
	}

	/** Refuses unsafe, stale, synthetic, and unsupported possession targets. */
	public static boolean isSuitable(TargetKind kind, boolean self, boolean alive,
			boolean removed, boolean bodyProxy) {
		return kind != TargetKind.OTHER && !self && alive && !removed && !bodyProxy;
	}

	/** Player hosts remain consent-gated; mobs do not own player consent state. */
	public static boolean requiresPlayerConsent(TargetKind kind) {
		return kind == TargetKind.PLAYER;
	}

	/** Cameras cannot track an entity across a world transition safely. */
	public static boolean sessionLocationValid(boolean ownerIsCurrent, boolean sameDimension) {
		return ownerIsCurrent && sameDimension;
	}

	/** Possession lasts at most thirty seconds even when innate duration scaling is high. */
	public static int durationTicks(int requestedTicks) {
		return Math.clamp(requestedTicks, 1, MAX_DURATION_TICKS);
	}

	/** A player cannot dominate another player whose active rank exceeds their own. */
	public static boolean rankAllows(int casterRank, int targetRank) {
		return casterRank >= 0 && targetRank >= 0 && casterRank >= targetRank;
	}

	/** Only Dreamwalking may carry the controller's mind to a host in another dimension. */
	public static boolean allowsCrossDimension(SessionKind kind) {
		return kind == SessionKind.DREAMWALK;
	}

	/** Dreamwalking retains its separately configurable consent channel. */
	public static boolean usesDreamwalkProtection(SessionKind kind) {
		return kind == SessionKind.DREAMWALK;
	}

	/** Both forms of mind control respect higher-rank player counterplay. */
	public static boolean requiresRankCheck(SessionKind kind) {
		return kind != null;
	}
}
