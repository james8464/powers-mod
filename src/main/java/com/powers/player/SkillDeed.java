package com.powers.player;

/** Persistent server-authenticated mastery counters for normal progression. */
public enum SkillDeed {
	POWER_USE("power_use"),
	POWER_KILL("power_kill"),
	BOSS_KILL("boss_kill"),
	LIGHT_MEMORY("light_memory");

	private final String key;

	SkillDeed(String key) {
		this.key = key;
	}

	public String key() {
		return key;
	}

	static SkillDeed fromKey(String key) {
		for (SkillDeed deed : values()) if (deed.key.equals(key)) return deed;
		return null;
	}
}
