package com.powers.realm;

/** Pure faction and persistent respawn timing for the two realm Heralds. */
public final class RealmHeraldRules {
	public static final long RESPAWN_DELAY_TICKS = 20L * 60L * 20L;

	private RealmHeraldRules() {
	}

	public static boolean mayTarget(RealmKind kind, boolean targetDarkness) {
		return kind == RealmKind.DARK ? !targetDarkness : targetDarkness;
	}

	public static long nextSpawnTime(long defeatedAt) {
		return Math.addExact(defeatedAt, RESPAWN_DELAY_TICKS);
	}

	public static boolean maySpawn(long gameTime, long nextSpawnAt) {
		return gameTime >= nextSpawnAt;
	}
}
