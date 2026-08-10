package com.powers.realm;

/** Fixed lore-memory location, its explained rank echo, and discovery reward. */
public record MemorySite(String id, int x, int z, String offeredPath) {
	public static final int DISCOVERY_REWARD_ENERGY = 12;

	public String memoryKey() {
		return "realm.powers." + id;
	}

	public String pathKey() {
		return "realm.powers.path." + offeredPath;
	}

	public int rewardEnergy() {
		return DISCOVERY_REWARD_ENERGY;
	}
}
