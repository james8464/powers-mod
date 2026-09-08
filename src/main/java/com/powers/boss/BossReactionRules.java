package com.powers.boss;

/** Pure, finite matrix for authored boss reactions to aligned setup actions. */
public final class BossReactionRules {
	public static final int COOLDOWN_TICKS = 60;
	public static final int ARTIFACT_RANGE = 24;
	public static final int MAX_BOSS_CANDIDATES = 16;

	public enum BossKind {
		LIGHT_HERALD,
		DARK_HERALD,
		FIRST_VESSEL
	}

	public enum Stimulus {
		LIGHT_ARTIFACT,
		DARK_ARTIFACT,
		RADIANT_CONCORD,
		UMBRAL_CONCORD
	}

	public enum Reaction {
		NONE,
		OPPOSITION_STAGGER,
		COVENANT_BREACH
	}

	private BossReactionRules() {
	}

	public static Reaction reaction(BossKind boss, Stimulus stimulus) {
		if (boss == BossKind.FIRST_VESSEL) return Reaction.COVENANT_BREACH;
		boolean darkStimulus = stimulus == Stimulus.DARK_ARTIFACT
				|| stimulus == Stimulus.UMBRAL_CONCORD;
		return darkStimulus == (boss == BossKind.LIGHT_HERALD)
				? Reaction.OPPOSITION_STAGGER : Reaction.NONE;
	}

	public static boolean offCooldown(long now, long lastReactionAt) {
		return lastReactionAt == Long.MIN_VALUE || now - lastReactionAt >= COOLDOWN_TICKS;
	}

	public static int reactionDurationTicks(Reaction reaction) {
		return switch (reaction) {
			case NONE -> 0;
			case OPPOSITION_STAGGER -> 100;
			case COVENANT_BREACH -> 160;
		};
	}
}
