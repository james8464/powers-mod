package com.powers.thermal;

import com.powers.PowersBlocks;
import com.powers.protection.PowerProtection;
import com.powers.util.LoadedChunks;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;

/** Classifies, plans, authorizes, and commits bounded thermal block changes. */
public final class ThermalWorldService {
	private ThermalWorldService() {
	}

	public static ThermalMaterial classify(BlockState state) {
		if (state.is(PowersBlocks.PURE_LIGHT)) return ThermalMaterial.PURE_LIGHT;
		if (state.is(PowersBlocks.DARKNESS)) return ThermalMaterial.DARKNESS;
		if (state.isAir()) return ThermalMaterial.AIR;
		if (state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE)) return ThermalMaterial.FIRE;
		if (state.getFluidState().is(FluidTags.WATER)) return ThermalMaterial.WATER;
		if (state.getFluidState().is(FluidTags.LAVA)) return ThermalMaterial.LAVA;
		if (state.is(BlockTags.ICE)) return ThermalMaterial.ICE;
		if (state.is(BlockTags.SNOW)) return ThermalMaterial.SNOW;
		if (state.getBlock() instanceof BonemealableBlock) return ThermalMaterial.PLANT;
		return ThermalMaterial.OTHER;
	}

	/** Returns no plan for stable cells or unsupported placement. */
	public static Optional<ThermalBlockPlan> plan(ServerLevel level, BlockPos position,
			ThermalInfluence influence) {
		if (!LoadedChunks.contains(level, position)) return Optional.empty();
		BlockPos immutable = position.immutable();
		BlockState observed = level.getBlockState(immutable);
		ThermalTransition transition = ThermalTransitionTable.resolve(influence, classify(observed));
		BlockState replacement = replacement(transition.outcome());
		if (transition.outcome() == ThermalOutcome.KEEP) return Optional.empty();
		if ((transition.outcome() == ThermalOutcome.IGNITE
				|| transition.outcome() == ThermalOutcome.COAT)
				&& (replacement == null || !replacement.canSurvive(level, immutable))) {
			return Optional.empty();
		}
		if (transition.outcome() == ThermalOutcome.GROW
				&& (!(observed.getBlock() instanceof BonemealableBlock growable)
				|| !growable.isValidBonemealTarget(level, immutable, observed))) {
			return Optional.empty();
		}
		return Optional.of(new ThermalBlockPlan(immutable, observed, transition, replacement));
	}

	public static boolean authorized(LivingEntity actor, ServerLevel level, ThermalBlockPlan plan) {
		return LoadedChunks.contains(level, plan.position())
				&& PowerProtection.mayAffectBlock(actor, level, plan.position());
	}

	/** Applies only the exact observed plan and never schedules follow-up work itself. */
	public static boolean commit(ServerLevel level, ThermalBlockPlan plan) {
		if (!LoadedChunks.contains(level, plan.position())
				|| !level.getBlockState(plan.position()).equals(plan.observed())) return false;
		if (plan.transition().outcome() == ThermalOutcome.GROW) {
			if (!(plan.observed().getBlock() instanceof BonemealableBlock growable)
					|| !growable.isValidBonemealTarget(level, plan.position(), plan.observed())) return false;
			growable.performBonemeal(level, level.getRandom(), plan.position(), plan.observed());
			return true;
		}
		return plan.replacement() != null
				&& level.setBlockAndUpdate(plan.position(), plan.replacement());
	}

	public static boolean apply(LivingEntity actor, ServerLevel level, BlockPos position,
			ThermalInfluence influence) {
		Optional<ThermalBlockPlan> plan = plan(level, position, influence);
		return plan.isPresent() && authorized(actor, level, plan.get()) && commit(level, plan.get());
	}

	/** Resolves the struck material before creating fire in supported air above it. */
	public static boolean applySurface(LivingEntity actor, ServerLevel level, BlockPos position,
			ThermalInfluence influence) {
		if (!LoadedChunks.contains(level, position)) return false;
		BlockState state = level.getBlockState(position);
		if (state.isAir() && LoadedChunks.contains(level, position.below())
				&& classify(level.getBlockState(position.below())) != ThermalMaterial.OTHER) {
			return apply(actor, level, position.below(), influence);
		}
		return apply(actor, level, position, influence);
	}

	private static BlockState replacement(ThermalOutcome outcome) {
		return switch (outcome) {
			case IGNITE -> Blocks.FIRE.defaultBlockState();
			case COAT -> Blocks.SNOW.defaultBlockState();
			case EXTINGUISH, EVAPORATE, SCORCH -> Blocks.AIR.defaultBlockState();
			case FREEZE -> Blocks.FROSTED_ICE.defaultBlockState();
			case SOLIDIFY -> Blocks.OBSIDIAN.defaultBlockState();
			case MELT -> Blocks.WATER.defaultBlockState();
			case KEEP, GROW -> null;
		};
	}
}
