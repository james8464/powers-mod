package com.powers.force;

import com.powers.PowersBlocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Identifies the two opposed kinds of spreading realm matter. */
public enum LivingForceKind {
	DARKNESS,
	PURE_LIGHT;

	/** Returns the registered block that embodies this force. */
	public Block block() {
		return this == DARKNESS ? PowersBlocks.DARKNESS : PowersBlocks.PURE_LIGHT;
	}

	/** Resolves a force block state, or {@code null} for ordinary terrain. */
	public static LivingForceKind from(BlockState state) {
		if (state.is(PowersBlocks.DARKNESS)) return DARKNESS;
		if (state.is(PowersBlocks.PURE_LIGHT)) return PURE_LIGHT;
		return null;
	}
}
