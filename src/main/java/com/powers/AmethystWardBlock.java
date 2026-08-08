package com.powers;

import com.powers.power.AmethystDampening;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.core.particles.ParticleTypes;

/**
 * A redstone-powered amethyst block. While it receives a signal it glows
 * with a spinning ring of sparks and, through the dampening rules, shuts
 * off players' powers within range
 */
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
		super.neighborChanged(state, level, pos, block, orientation, moving);
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		updatePower(state, level, pos);
		super.onPlace(state, level, pos, oldState, movedByPiston);
	}

	private void updatePower(BlockState state, Level level, BlockPos pos) {
		int power = level.getBestNeighborSignal(pos);
		// only rewrite the block when the signal actually changed, to avoid pointless block updates
		if (state.getValue(BlockStateProperties.POWER) != power) {
			level.setBlock(pos, state.setValue(BlockStateProperties.POWER, power), Block.UPDATE_CLIENTS);
		}
		// tell the dampening index either way, then keep the heartbeat running
		// while the ward is live. the tick loop is what re-registers the ward
		// after a server restart, since the in-memory index starts out empty and
		// scheduled block ticks are saved with the chunk
		syncDampeningIndex(level, pos);
	}

	private void syncDampeningIndex(Level level, BlockPos pos) {
		if (!(level instanceof ServerLevel serverLevel)) {
			return;
		}
		if (isPowered(serverLevel.getBlockState(pos))) {
			AmethystDampening.addPoweredWard(serverLevel, pos);
			serverLevel.scheduleTick(pos, this, 5);
		} else {
			AmethystDampening.removePoweredWard(serverLevel, pos);
		}
	}

	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean moving) {
		// a broken or piston-pushed ward stops projecting immediately
		AmethystDampening.removePoweredWard(level, pos);
		super.affectNeighborsAfterRemoval(state, level, pos, moving);
	}

	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (!isPowered(state)) {
			AmethystDampening.removePoweredWard(level, pos);
			return;
		}
		// re-assert membership on every beat so the index heals itself after a
		// reload without anyone having to nudge the redstone
		AmethystDampening.addPoweredWard(level, pos);
		// 0.08 radians per tick makes the ring spin slowly
		double phase = level.getServer().getTickCount() * 0.08;
		// four sparks, one per quarter turn, orbiting the block
		for (int i = 0; i < 4; i++) {
			double angle = phase + Math.PI * 2.0 * i / 4.0;
			com.powers.fx.PowerFx.burst(level, new net.minecraft.world.phys.Vec3(
					pos.getX() + 0.5 + Math.cos(angle) * 0.9, pos.getY() + 0.5,
					pos.getZ() + 0.5 + Math.sin(angle) * 0.9), ParticleTypes.END_ROD, 1, 0, 0);
		}
		// schedule the next ring in 5 ticks to keep the loop going
		level.scheduleTick(pos, this, 5);
	}

	/** whether the ward is currently receiving redstone power */
	public static boolean isPowered(BlockState state) {
		return state.hasProperty(BlockStateProperties.POWER)
				&& state.getValue(BlockStateProperties.POWER) > 0;
	}
}
