package com.powers.power.artifact;

import com.powers.PowersEntities;
import com.powers.entity.AbstractPlayerLikeMob;
import com.powers.fx.PowerFx;
import com.powers.item.artifact.ArtifactAlignment;
import com.powers.protection.PowerProtection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Creates and indexes bounded owner-aware darkness and radiant guardians. */
public final class ArtifactGuardianSummons {
	private static final int NORMAL_LIFETIME = 20 * 60 * 3;
	private static final int ELITE_LIFETIME = 20 * 60;
	private static final Map<UUID, Set<UUID>> NORMAL_BY_OWNER = new HashMap<>();
	private static final Map<UUID, Set<UUID>> ELITE_BY_OWNER = new HashMap<>();
	private static final Set<UUID> LOADED = new HashSet<>();

	private ArtifactGuardianSummons() {
	}

	public static int summon(ServerPlayer caster, ArtifactAlignment alignment,
			int requested, boolean elite, LivingEntity forcedTarget, boolean owned) {
		ServerLevel level = (ServerLevel) caster.level();
		if (PowerProtection.isSafeZone(level, caster.position())) return 0;
		Map<UUID, Set<UUID>> index = elite ? ELITE_BY_OWNER : NORMAL_BY_OWNER;
		Set<UUID> existing = index.computeIfAbsent(caster.getUUID(), ignored -> new HashSet<>());
		int allowed = ArtifactDominionRules.guardiansToSpawn(requested, existing.size(), elite);
		allowed = Math.min(allowed, Math.max(0,
				ArtifactDominionRules.MAX_LOADED_GUARDIANS - LOADED.size()));
		int spawned = 0;
		for (int attempt = 0; attempt < allowed * 4 && spawned < allowed; attempt++) {
			BlockPos spawnPos = radialSpawn(level, caster, attempt, allowed);
			if (spawnPos == null) continue;
			AbstractPlayerLikeMob guardian = alignment == ArtifactAlignment.DARKNESS
					? PowersEntities.DARKNESS_CREATURE.create(level, EntitySpawnReason.MOB_SUMMONED)
					: PowersEntities.RADIANT_SENTINEL.create(level, EntitySpawnReason.MOB_SUMMONED);
			if (guardian == null) continue;
			guardian.setPos(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
			guardian.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos),
					EntitySpawnReason.MOB_SUMMONED, null);
			guardian.configureGuardian(owned ? caster.getUUID() : null,
					elite ? ELITE_LIFETIME : NORMAL_LIFETIME, elite);
			if (forcedTarget != null) guardian.setTarget(forcedTarget);
			if (!level.addFreshEntity(guardian) || guardian.isRemoved()) continue;
			existing.add(guardian.getUUID());
			var lightning = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
			if (lightning != null) {
				lightning.setVisualOnly(true);
				lightning.setPos(guardian.position());
				level.addFreshEntity(lightning);
			}
			arrival(level, guardian, alignment, elite);
			spawned++;
		}
		if (existing.isEmpty()) index.remove(caster.getUUID());
		return spawned;
	}

	private static void arrival(ServerLevel level, AbstractPlayerLikeMob guardian,
			ArtifactAlignment alignment, boolean elite) {
		int color = alignment == ArtifactAlignment.DARKNESS ? 0x3A0B52 : 0xFFE89B;
		PowerFx.rune(level, guardian.position(), elite ? 3.0 : 1.7, color, elite ? 44 : 26, 0.0);
		PowerFx.spiral(level, guardian.position(), elite ? 1.8 : 0.8, elite ? 5.0 : 2.5,
				alignment == ArtifactAlignment.DARKNESS ? 0x6C2383 : 0xFFFFFF,
				elite ? 42 : 22, Math.PI / 8.0);
	}

	private static BlockPos radialSpawn(ServerLevel level, ServerPlayer caster, int attempt, int requested) {
		double angle = (attempt + level.getRandom().nextDouble()) * Math.PI * 2.0
				/ Math.max(1, requested);
		int x = caster.getBlockX() + (int) Math.round(Math.cos(angle) * (3.0 + attempt % 3));
		int z = caster.getBlockZ() + (int) Math.round(Math.sin(angle) * (3.0 + attempt % 3));
		BlockPos around = new BlockPos(x, caster.getBlockY(), z);
		for (int offset = 3; offset >= -5; offset--) {
			BlockPos feet = around.offset(0, offset, 0);
			BlockPos floor = feet.below();
			if (level.getBlockState(feet).isAir() && level.getBlockState(feet.above()).isAir()
					&& level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP)) return feet;
		}
		return null;
	}

	/** Rebuilds loaded-session caps after chunk loads and server restarts. */
	public static void trackLoaded(AbstractPlayerLikeMob guardian) {
		if (!guardian.temporaryGuardian() || LOADED.contains(guardian.getUUID())) return;
		UUID owner = guardian.guardianOwner();
		Map<UUID, Set<UUID>> index = guardian.eliteGuardian() ? ELITE_BY_OWNER : NORMAL_BY_OWNER;
		Set<UUID> owned = owner == null ? null : index.computeIfAbsent(owner, ignored -> new HashSet<>());
		int ownedCount = owned == null ? 0 : owned.size();
		if (!ArtifactDominionRules.guardianCanLoad(LOADED.size(), ownedCount,
				guardian.eliteGuardian())) {
			if (owned != null && owned.isEmpty()) index.remove(owner);
			guardian.discard();
			return;
		}
		LOADED.add(guardian.getUUID());
		if (owned != null) owned.add(guardian.getUUID());
	}

	/** Chunk unloads release active-work capacity; a later load is revalidated. */
	public static void untrackLoaded(AbstractPlayerLikeMob guardian) {
		UUID id = guardian.getUUID();
		if (!LOADED.remove(id)) return;
		for (Map<UUID, Set<UUID>> index : java.util.List.of(NORMAL_BY_OWNER, ELITE_BY_OWNER)) {
			index.values().forEach(ids -> ids.remove(id));
			index.entrySet().removeIf(entry -> entry.getValue().isEmpty());
		}
	}

	public static void clear() {
		NORMAL_BY_OWNER.clear();
		ELITE_BY_OWNER.clear();
		LOADED.clear();
	}

	public static int indexedGuardianCount() {
		return LOADED.size();
	}
}
