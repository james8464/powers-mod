package com.powers.spell;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Pure classification and hard work caps for Verdant Tending. */
public final class VerdantTendingRules {
	public static final int MAX_INSPECTED_BLOCKS = 192;
	public static final int MAX_CHANGED_BLOCKS = 64;

	public enum Action { NONE, GROW, HYDRATE, EXTINGUISH }

	private VerdantTendingRules() {
	}

	public static Action action(BlockState state) {
		if (state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE)) return Action.EXTINGUISH;
		if (state.is(Blocks.FARMLAND)
				&& state.getValue(FarmlandBlock.MOISTURE) < FarmlandBlock.MAX_MOISTURE) return Action.HYDRATE;
		return state.getBlock() instanceof BonemealableBlock ? Action.GROW : Action.NONE;
	}
}
