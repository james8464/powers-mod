package com.powers.player;

/** Persistent categories counted by the darkness progression rites. */
public enum DarknessDeed {
	PASSIVE("passive"),
	VILLAGER("villager"),
	BABY_VILLAGER("baby_villager"),
	WOLF("wolf"),
	IRON_GOLEM("iron_golem");

	private final String key;

	DarknessDeed(String key) {
		this.key = key;
	}

	/** Stable attachment key used in player saves. */
	public String key() {
		return key;
	}

	/** Reads a saved key while safely ignoring data from removed future rites. */
	public static DarknessDeed fromKey(String key) {
		for (DarknessDeed deed : values()) {
			if (deed.key.equals(key)) {
				return deed;
			}
		}
		return null;
	}
}
