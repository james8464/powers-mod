package com.powers.realm;

import com.powers.allegiance.AllegianceMatrix;
import com.powers.allegiance.AllegianceReloadListener;

/** Pure faction and persistent respawn timing for the two realm Heralds. */
public final class RealmHeraldRules {
	public static final long RESPAWN_DELAY_TICKS = 20L * 60L * 20L;

	private RealmHeraldRules() {
	}

	public static boolean mayTarget(RealmKind kind, boolean targetDarkness) {
		AllegianceMatrix.Faction source = kind == RealmKind.DARK
				? AllegianceMatrix.Faction.DARKNESS : AllegianceMatrix.Faction.RADIANT;
		AllegianceMatrix.Faction target = targetDarkness
				? AllegianceMatrix.Faction.DARKNESS : AllegianceMatrix.Faction.RADIANT;
		return AllegianceReloadListener.snapshot().matrix().resolve(
				new AllegianceMatrix.Profile(source, AllegianceMatrix.Role.BOSS),
				new AllegianceMatrix.Profile(target, AllegianceMatrix.Role.PLAYER), false)
				== AllegianceMatrix.Relationship.HOSTILE;
	}

	public static long nextSpawnTime(long defeatedAt) {
		return Math.addExact(defeatedAt, RESPAWN_DELAY_TICKS);
	}

	public static boolean maySpawn(long gameTime, long nextSpawnAt) {
		return gameTime >= nextSpawnAt;
	}
}
