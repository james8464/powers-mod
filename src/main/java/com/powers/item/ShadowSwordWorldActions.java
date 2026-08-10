package com.powers.item;

import com.powers.PowersBlocks;
import com.powers.PowersEntities;
import com.powers.entity.DarknessCreature;
import com.powers.fx.ShadowSwordFx;
import com.powers.force.LivingForceManager;
import com.powers.force.LivingForceRules;
import com.powers.protection.PowerProtection;
import com.powers.util.BoundedEntityCandidates;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

/** Safe-zone-aware entity summoning and bounded ground corruption for the sword. */
final class ShadowSwordWorldActions {
	private ShadowSwordWorldActions() {
	}

	static int summonGuardians(ServerPlayer caster, LivingEntity forcedTarget, int requested) {
		ServerLevel level = (ServerLevel) caster.level();
		if (PowerProtection.isSafeZone(level, caster.position())) return 0;
		int spawned = 0;
		for (int attempt = 0; attempt < requested * 3 && spawned < requested; attempt++) {
			double angle = (attempt + level.getRandom().nextDouble()) * Math.PI * 2.0 / Math.max(1, requested);
			int x = caster.getBlockX() + (int) Math.round(Math.cos(angle) * (3.0 + attempt % 2));
			int z = caster.getBlockZ() + (int) Math.round(Math.sin(angle) * (3.0 + attempt % 2));
			BlockPos spawnPos = findSpawn(level, new BlockPos(x, caster.getBlockY(), z));
			if (spawnPos == null) continue;
			DarknessCreature guardian = PowersEntities.DARKNESS_CREATURE.create(level,
					EntitySpawnReason.MOB_SUMMONED);
			if (guardian == null) continue;
			guardian.setPos(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
			guardian.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos),
					EntitySpawnReason.MOB_SUMMONED, null);
			if (forcedTarget != null) guardian.setTarget(forcedTarget);
			if (!level.addFreshEntity(guardian)) continue;
			var bolt = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
			if (bolt != null) {
				bolt.setVisualOnly(true);
				bolt.setPos(guardian.position());
				level.addFreshEntity(bolt);
			}
			ShadowSwordFx.guardianArrival(level, guardian.position());
			spawned++;
		}
		return spawned;
	}

	static int nearbyGuardians(ServerPlayer player, double radius) {
		ServerLevel level = (ServerLevel) player.level();
		return BoundedEntityCandidates.ofClass(level, DarknessCreature.class,
				AABB.ofSize(player.position(), radius * 2.0, radius, radius * 2.0),
				ShadowSwordRules.MAX_COMMANDED_GUARDIANS + 1,
				DarknessCreature::isAlive).size();
	}

	static int spreadDarkness(ServerPlayer caster) {
		ServerLevel level = (ServerLevel) caster.level();
		int changed = 0;
		BlockPos origin = caster.blockPosition().below();
		for (int dx = -ShadowSwordRules.SPREAD_RADIUS; dx <= ShadowSwordRules.SPREAD_RADIUS; dx++) {
			for (int dz = -ShadowSwordRules.SPREAD_RADIUS; dz <= ShadowSwordRules.SPREAD_RADIUS; dz++) {
				if (!ShadowSwordRules.inSpreadDisc(dx, dz)) continue;
				BlockPos ground = findGround(level, origin.offset(dx, 0, dz));
				if (ground == null || PowerProtection.isSafeZone(level,
						net.minecraft.world.phys.Vec3.atCenterOf(ground))) continue;
				var state = level.getBlockState(ground);
				if (state.is(PowersBlocks.DARKNESS) || state.is(PowersBlocks.PURE_LIGHT)
						|| !LivingForceRules.mayReplace(state.isAir(), !state.getFluidState().isEmpty(),
								level.getBlockEntity(ground) != null,
								state.is(LivingForceManager.FORCE_SPREAD_IMMUNE),
								state.getDestroySpeed(level, ground))) continue;
				if (level.setBlock(ground, PowersBlocks.DARKNESS.defaultBlockState(), 3)) changed++;
			}
		}
		if (changed > 0) ShadowSwordFx.spread(level, caster.position(), changed);
		return changed;
	}

	private static BlockPos findSpawn(ServerLevel level, BlockPos around) {
		for (int offset = 2; offset >= -4; offset--) {
			BlockPos feet = around.offset(0, offset, 0);
			BlockPos floor = feet.below();
			if (level.getBlockState(feet).isAir() && level.getBlockState(feet.above()).isAir()
					&& level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP)) return feet;
		}
		return null;
	}

	private static BlockPos findGround(ServerLevel level, BlockPos around) {
		for (int offset = 2; offset >= -5; offset--) {
			BlockPos candidate = around.offset(0, offset, 0);
			if (!level.getBlockState(candidate).isAir()
					&& level.getBlockState(candidate.above()).isAir()) return candidate;
		}
		return null;
	}
}
