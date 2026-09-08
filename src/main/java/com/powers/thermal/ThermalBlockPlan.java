package com.powers.thermal;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** Immutable observed-state plan; commit refuses to reinterpret changed terrain. */
public record ThermalBlockPlan(BlockPos position, BlockState observed,
		ThermalTransition transition, BlockState replacement) {
}
