package com.powers;

import com.powers.power.AmethystDampening;
import com.powers.force.ForceContainmentManager;
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
	private static final int INDEX_HEARTBEAT_TICKS = 200;

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
			ForceContainmentManager.request(serverLevel, pos);
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
		ForceContainmentManager.request(level, pos);
		// This slow heartbeat only repairs the transient index after reloads.
		// Visible particles are expanded locally in animateTick below.
		level.scheduleTick(pos, this, INDEX_HEARTBEAT_TICKS);
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (!isPowered(state)) return;
		double angle = (level.getGameTime() + random.nextDouble() * 8.0) * 0.08;
		double radius = 0.78 + random.nextDouble() * 0.16;
		level.addParticle(com.powers.PowersParticles.GLYPH,
				pos.getX() + 0.5 + Math.cos(angle) * radius,
				pos.getY() + 0.35 + random.nextDouble() * 0.3,
				pos.getZ() + 0.5 + Math.sin(angle) * radius,
				0.0, 0.01, 0.0);
	}

	/** whether the ward is currently receiving redstone power */
	public static boolean isPowered(BlockState state) {
		return state.hasProperty(BlockStateProperties.POWER)
				&& state.getValue(BlockStateProperties.POWER) > 0;
	}
}
