package com.powers.boss;

/** Transient per-entity reaction state; it cannot outlive or leak beyond its boss. */
public interface BossReactionTarget {
	BossReactionRules.BossKind bossReactionKind();

	long lastBossReactionAt();

	void markBossReaction(long tick);
}
