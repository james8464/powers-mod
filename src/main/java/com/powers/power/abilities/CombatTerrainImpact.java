package com.powers.power.abilities;

import com.powers.PowersBlocks;
import com.powers.fx.PowerFx;
import com.powers.item.ArtifactWeaponManager;
import com.powers.config.PowersConfigLoader;
import com.powers.item.artifact.ArtifactAlignment;
import com.powers.magic.runtime.CastSource;
import com.powers.player.PlayerPowers;
import com.powers.player.SkillSystem;
import com.powers.progression.InnatePowerLevels;
import com.powers.power.AmethystDampening;
import com.powers.protection.PowerProtection;
import com.powers.util.LoadedChunks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Bounded loaded-chunk terrain mutations shared by boss-scale innate attacks. */
public final class CombatTerrainImpact {
	private CombatTerrainImpact() {
	}

	/** Resolves the authored terrain tier without leaking innate rank into spells or crystals. */
	public static int tier(ServerPlayer player, CastSource source, String actionId) {
		if (source == CastSource.INNATE) return CombatTerrainRules.effectiveTier(
				InnatePowerLevels.forPower(actionId, SkillSystem.effectiveLevel(player)).destructionTier(),
				PowersConfigLoader.get().terrainScars().minimumTier());
		if (source != CastSource.ARTIFACT) return 0;
		if (ArtifactWeaponManager.holds(player, ArtifactAlignment.DARKNESS)
				&& ArtifactWeaponManager.authorized(player, ArtifactAlignment.DARKNESS)) {
			return PlayerPowers.get(player).darknessLevel();
		}
		if (ArtifactWeaponManager.holds(player, ArtifactAlignment.LIGHT)
				&& ArtifactWeaponManager.authorized(player, ArtifactAlignment.LIGHT)) {
			return PlayerPowers.get(player).skillLevel();
		}
		return 0;
	}

	/** Removes a hard-capped crater around one loaded impact point. */
	public static int crater(ServerLevel level, ServerPlayer caster, Vec3 center, int rank) {
		return craterLiving(level, caster, center, rank);
	}

	/** Max-rank entity-safe crater used by Shadow and boss-scale actors. */
	public static int craterLiving(ServerLevel level, LivingEntity caster, Vec3 center, int rank) {
		int budget = budget(CombatTerrainRules.craterBudget(rank));
		double radius = CombatTerrainRules.craterRadius(rank);
		BlockPos origin = BlockPos.containing(center);
		int removed = 0;
		for (int index = 0; index < budget * 3 && removed < budget; index++) {
			double radial = radius * Math.cbrt((index + 0.5) / (budget * 3.0));
			double angle = index * 2.399963229728653;
			int dx = (int) Math.round(Math.cos(angle) * radial);
			int dz = (int) Math.round(Math.sin(angle) * radial);
			int dy = -(index % Math.max(1, 1 + rank / 3));
			BlockPos candidate = origin.offset(dx, dy, dz);
			BlockPos block = solidNear(level, candidate, 2);
			if (block != null && breakBlock(level, caster, block, rank)) removed++;
		}
		if (removed > 0) PowerFx.coloredBurst(level, center, 0x5D3928,
				Math.min(18, 4 + removed / 6), Math.min(2.5, radius * 0.5));
		return removed;
	}

	/** Cuts a sparse, widening pressure scar through loaded blocks in front of the caster. */
	public static int thunderclap(ServerLevel level, ServerPlayer caster, Vec3 origin,
			Vec3 direction, double range, int rank) {
		Vec3 horizontal = new Vec3(direction.x, 0.0, direction.z);
		if (horizontal.lengthSqr() < 1.0E-8) return 0;
		horizontal = horizontal.normalize();
		Vec3 side = new Vec3(-horizontal.z, 0.0, horizontal.x);
		int budget = budget(CombatTerrainRules.thunderclapBudget(rank));
		int removed = 0;
		for (int index = 0; index < budget * 2 && removed < budget; index++) {
			double progress = (index + 1.0) / (budget * 2.0);
			double distance = 2.0 + progress * Math.max(2.0, range - 2.0);
			double width = Math.max(1.0, distance * 0.35);
			double lateral = ((index * 37 % 101) / 50.0 - 1.0) * width;
			Vec3 sample = origin.add(horizontal.scale(distance)).add(side.scale(lateral));
			BlockPos block = solidNear(level, BlockPos.containing(sample), 5);
			if (block != null && breakBlock(level, caster, block, rank)) removed++;
		}
		if (removed > 0) PowerFx.coloredBurst(level, origin.add(horizontal.scale(3.0)),
				0xD8EEF5, Math.min(16, 5 + removed / 5), 1.3);
		return removed;
	}

	/** Burns a sparse block trail backward from a beam's terminal point. */
	public static int rayScar(ServerLevel level, ServerPlayer caster, Vec3 from, Vec3 to,
			int rank, int color) {
		return rayScarLiving(level, caster, from, to, rank, color);
	}

	public static int rayScarLiving(ServerLevel level, LivingEntity caster, Vec3 from, Vec3 to,
			int rank, int color) {
		Vec3 delta = to.subtract(from);
		if (delta.lengthSqr() < 1.0E-8) return 0;
		int budget = budget(CombatTerrainRules.rayBudget(rank));
		int removed = 0;
		for (int index = 0; index < budget * 3 && removed < budget; index++) {
			double fraction = 1.0 - index / (double) Math.max(1, budget * 3);
			BlockPos candidate = BlockPos.containing(from.add(delta.scale(fraction)));
			BlockPos block = solidNear(level, candidate, 1);
			if (block != null && breakBlock(level, caster, block, rank)) removed++;
		}
		if (removed > 0) PowerFx.coloredBurst(level, to, color,
				Math.min(12, 3 + removed), 0.65 + removed * 0.08);
		return removed;
	}

	private static BlockPos solidNear(ServerLevel level, BlockPos origin, int verticalSearch) {
		for (int offset = verticalSearch; offset >= -verticalSearch; offset--) {
			BlockPos candidate = origin.offset(0, offset, 0);
			if (!LoadedChunks.contains(level, candidate)) return null;
			if (!level.getBlockState(candidate).isAir()) return candidate;
		}
		return null;
	}

	private static int budget(int requested) {
		return CombatTerrainRules.cappedBudget(requested,
				PowersConfigLoader.get().terrainScars().maxBlocksPerCast());
	}

	private static boolean breakBlock(ServerLevel level, LivingEntity caster, BlockPos pos, int rank) {
		BlockState state = level.getBlockState(pos);
		if (state.isAir() || !PowerProtection.mayAffectBlock(caster, level, pos)
				|| state.is(Blocks.BEDROCK) || state.is(Blocks.END_PORTAL_FRAME)
				|| state.is(Blocks.END_PORTAL) || state.is(Blocks.NETHER_PORTAL)
				|| state.is(PowersBlocks.DARKNESS) || state.is(PowersBlocks.PURE_LIGHT)
				|| state.is(AmethystDampening.AMETHYST_BLOCKS)
				|| !state.getFluidState().isEmpty()) return false;
		float hardness = state.getDestroySpeed(level, pos);
		return Float.isFinite(hardness) && hardness >= 0.0F
				&& hardness <= CombatTerrainRules.maximumHardness(rank)
				&& level.destroyBlock(pos, false, caster);
	}

	/** Converts a capped disc into living Darkness without bypassing protection or loaded chunks. */
	public static int blightDisc(ServerLevel level, LivingEntity caster, int radius, int budget) {
		BlockPos origin = caster.blockPosition().below();
		int converted = 0;
		for (int index = 0; index < budget * 3 && converted < budget; index++) {
			double radial = radius * Math.sqrt((index + 0.5) / (budget * 3.0));
			double angle = index * 2.399963229728653;
			BlockPos sample = origin.offset((int) Math.round(Math.cos(angle) * radial), 0,
					(int) Math.round(Math.sin(angle) * radial));
			BlockPos ground = solidNear(level, sample, 3);
			if (ground == null || !PowerProtection.mayAffectBlock(caster, level, ground)) continue;
			BlockState state = level.getBlockState(ground);
			if (!com.powers.force.LivingForceRules.mayReplace(state.isAir(),
					!state.getFluidState().isEmpty(), false,
					state.is(com.powers.force.LivingForceManager.FORCE_SPREAD_IMMUNE),
					state.getDestroySpeed(level, ground))) continue;
			level.setBlock(ground, PowersBlocks.DARKNESS.defaultBlockState(), 3);
			converted++;
		}
		return converted;
	}
}
