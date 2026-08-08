package com.powers.force;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;

/** Random-ticking realm-matter block whose mutations are delegated to the server manager. */
public final class LivingForceBlock extends Block {
	private final LivingForceKind kind;

	public LivingForceBlock(LivingForceKind kind, BlockBehaviour.Properties properties) {
		super(properties);
		this.kind = kind;
	}

	@Override
	protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		LivingForceManager.spread(level, pos, kind, random);
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		if (level instanceof ServerLevel serverLevel) {
			LivingForceManager.register(serverLevel, pos, kind);
			LivingForceManager.checkForClash(serverLevel, pos, kind);
		}
		super.onPlace(state, level, pos, oldState, movedByPiston);
	}

	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block,
			Orientation orientation, boolean movedByPiston) {
		if (level instanceof ServerLevel serverLevel) {
			LivingForceManager.checkForClash(serverLevel, pos, kind);
		}
		super.neighborChanged(state, level, pos, block, orientation, movedByPiston);
	}

	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos,
			boolean movedByPiston) {
		LivingForceManager.unregister(level, pos);
		super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
	}
}
