package com.powers.companion;

/** Pure item and owner facts consumed by the conjuration policy. */
public record ShadowConjurationFacts(int requestedCount, int maximumStack,
		ShadowConjurationTier tier, boolean trustedNamespace, boolean externalOptIn,
		boolean artifact, boolean adminOnly, boolean spawnEgg, boolean crystal,
		boolean darkCrystal, boolean testingBypass, int energy) {
	public ShadowConjurationFacts {
		requestedCount = Math.max(1, requestedCount);
		maximumStack = Math.clamp(maximumStack, 1, 64);
		tier = tier == null ? ShadowConjurationTier.COMMON : tier;
		energy = ShadowCompanionRules.energy(energy);
	}

	public ShadowConjurationFacts withTier(ShadowConjurationTier value) {
		return copy(value, artifact, adminOnly, spawnEgg, crystal, darkCrystal, energy);
	}

	public ShadowConjurationFacts withArtifact(boolean value) {
		return copy(tier, value, adminOnly, spawnEgg, crystal, darkCrystal, energy);
	}

	public ShadowConjurationFacts withAdminOnly(boolean value) {
		return copy(tier, artifact, value, spawnEgg, crystal, darkCrystal, energy);
	}

	public ShadowConjurationFacts withSpawnEgg(boolean value) {
		return copy(tier, artifact, adminOnly, value, crystal, darkCrystal, energy);
	}

	public ShadowConjurationFacts withCrystal(boolean value, boolean darkness) {
		return copy(tier, artifact, adminOnly, spawnEgg, value, darkness, energy);
	}

	public ShadowConjurationFacts withEnergy(int value) {
		return copy(tier, artifact, adminOnly, spawnEgg, crystal, darkCrystal, value);
	}

	private ShadowConjurationFacts copy(ShadowConjurationTier nextTier,
			boolean nextArtifact, boolean nextAdmin, boolean nextEgg,
			boolean nextCrystal, boolean nextDarkCrystal, int nextEnergy) {
		return new ShadowConjurationFacts(requestedCount, maximumStack, nextTier,
				trustedNamespace, externalOptIn, nextArtifact, nextAdmin, nextEgg,
				nextCrystal, nextDarkCrystal, testingBypass, nextEnergy);
	}
}
