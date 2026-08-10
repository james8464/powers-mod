package com.powers.spell;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;

import java.util.UUID;

/** Immutable target captured when a ritual begins, so channel completion cannot jump aim. */
record SpellTarget(UUID entityId, BlockPos blockPos, boolean available) {
	static SpellTarget none() {
		return new SpellTarget(null, null, true);
	}

	static SpellTarget missing() {
		return new SpellTarget(null, null, false);
	}

	static SpellTarget entity(LivingEntity target) {
		return target == null ? missing() : new SpellTarget(target.getUUID(), null, true);
	}

	static SpellTarget block(BlockPos target) {
		return target == null ? missing() : new SpellTarget(null, target.immutable(), true);
	}
}
