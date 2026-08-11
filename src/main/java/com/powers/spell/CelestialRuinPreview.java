package com.powers.spell;

import net.minecraft.core.BlockPos;

/** Immutable read-only Heavenfall staging report for operator review. */
public record CelestialRuinPreview(String dimension, BlockPos center, int craterChunks,
		int shockwaveChunks, int loadedEntityCandidates, boolean entityLimitReached,
		int intersectingProtectedRegions, boolean terrainDamage, boolean blockEntityDamage,
		boolean centerPermitted) {
}
