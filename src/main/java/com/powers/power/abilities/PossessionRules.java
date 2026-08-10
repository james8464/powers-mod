package com.powers.power.abilities;

/** Pure target eligibility and consent classification for Vessel Possession. */
public final class PossessionRules {
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
}
