package com.powers;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.core.particles.ParticleTypes;

public class AmethystWardBlock extends Block {
	public AmethystWardBlock(BlockBehaviour.Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(BlockStateProperties.POWER, 0));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(BlockStateProperties.POWER);
	}

	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block,
			net.minecraft.world.level.redstone.Orientation orientation, boolean moving) {
		updatePower(state, level, pos);
		if (isPowered(level.getBlockState(pos))) level.scheduleTick(pos, this, 5);
		super.neighborChanged(state, level, pos, block, orientation, moving);
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		updatePower(state, level, pos);
		super.onPlace(state, level, pos, oldState, movedByPiston);
	}

	private void updatePower(BlockState state, Level level, BlockPos pos) {
		int power = level.getBestNeighborSignal(pos);
		if (state.getValue(BlockStateProperties.POWER) != power) {
			level.setBlock(pos, state.setValue(BlockStateProperties.POWER, power), Block.UPDATE_CLIENTS);
		}
	}

	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (!isPowered(state)) return;
		double phase = level.getServer().getTickCount() * 0.08;
		for (int i = 0; i < 4; i++) {
			double angle = phase + Math.PI * 2.0 * i / 4.0;
			level.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5 + Math.cos(angle) * 0.9,
					pos.getY() + 0.5, pos.getZ() + 0.5 + Math.sin(angle) * 0.9, 1, 0, 0, 0, 0);
		}
		level.scheduleTick(pos, this, 5);
	}

	public static boolean isPowered(BlockState state) {
		return state.hasProperty(BlockStateProperties.POWER)
				&& state.getValue(BlockStateProperties.POWER) > 0;
	}
}
