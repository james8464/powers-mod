package com.powers.force;

import com.powers.PowersMod;
import com.powers.PowerStatusEffects;
import com.powers.PowersParticles;
import com.powers.PowersSounds;
import com.powers.player.PlayerPowers;
import com.powers.player.SkillSystem;
import com.powers.power.AmethystDampening;
import com.powers.power.state.GlobalTimeStopManager;
import com.powers.progression.PowerScalingService;
import com.powers.progression.RankVariantRules;
import com.powers.config.PowersConfig;
import com.powers.config.PowersConfigLoader;
import com.powers.fx.PowerFx;
import com.powers.protection.PowerProtection;
import com.powers.spell.SpellFieldManager;
import com.powers.network.PowersPackets;
import com.powers.magic.MagicActionId;
import com.powers.magic.runtime.MagicPresenceHandle;
import com.powers.magic.runtime.PhysicalMagicPresences;
import com.powers.util.BoundedEntityCandidates;
import com.powers.util.LoadedChunks;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

/** Owns loaded-force indexing and bounded server-side terrain spreading. */
public final class LivingForceManager {
	/** Blocks a datapack may protect from either living force. */
	public static final TagKey<Block> FORCE_SPREAD_IMMUNE =
			TagKey.create(Registries.BLOCK, PowersMod.id("living_force_immune"));

	private static final Map<ServerLevel, LivingForceIndex> INDEXES = new WeakHashMap<>();
	private static final Map<ServerLevel, List<ForceClashWave>> ACTIVE_CLASHES = new WeakHashMap<>();
	private static final int MAX_ACTIVE_CLASHES_PER_LEVEL = 4;
	private static final double PEAK_CLASH_DAMAGE = 100.0;
	private static final double PEAK_CLASH_IMPULSE = 8.0;
	private static final int MAX_AURA_CANDIDATES_PER_PLAYER = 512;
	private static final int MAX_AURA_CANDIDATES_PER_LEVEL = 4_096;
	private static final int MAX_AURA_PLAYERS_PER_LEVEL = 128;
	private static final double ACTIVE_ENTITY_SCAN_RADIUS = 128.0;
	private static final UUID WORLD_MAGIC_OWNER = new UUID(0L, 0L);

	private LivingForceManager() {
	}

	/** Registers chunk lifecycle hooks that rebuild and evict the spatial index. */
	public static void initialize() {
		ServerChunkEvents.CHUNK_LOAD.register((level, chunk, newlyGenerated) ->
				loadFrontier(level, chunk));
		ServerChunkEvents.CHUNK_UNLOAD.register((level, chunk) ->
				index(level).removeChunk(chunk.getPos().pack()));
	}

	static void register(ServerLevel level, BlockPos pos, LivingForceKind kind) {
		refreshFrontier(level, pos);
	}

	static void unregister(ServerLevel level, BlockPos pos) {
		refreshFrontier(level, pos);
		for (Direction direction : Direction.values()) refreshFrontier(level, pos.relative(direction));
	}

	static void checkForClash(ServerLevel level, BlockPos source, LivingForceKind kind) {
		for (Direction direction : Direction.values()) {
			BlockPos neighbor = source.relative(direction);
			if (!LoadedChunks.contains(level, neighbor)) continue;
			LivingForceKind other = LivingForceKind.from(level.getBlockState(neighbor));
			if (other != null && LivingForceRules.opposes(kind, other)) {
				requestClash(level, source);
				return;
			}
		}
	}

	/** Makes a few face-adjacent conversion attempts when vanilla selects a force block for a random tick. */
	static void spread(ServerLevel level, BlockPos source, LivingForceKind kind, RandomSource random) {
		register(level, source, kind);
		// Opposition remains active even when an administrator pauses conversion.
		checkForClash(level, source, kind);
		PowersConfig.LivingForces policy = PowersConfigLoader.get().livingForces();
		if (!policy.spreadingEnabled() || PowerProtection.isSafeZone(level, Vec3.atCenterOf(source))) return;
		for (int attempt = 0; attempt < policy.spreadAttempts(); attempt++) {
			BlockPos target = source.relative(Direction.getRandom(random));
			if (!LoadedChunks.contains(level, target)
					|| PowerProtection.isSafeZone(level, Vec3.atCenterOf(target))) continue;
			BlockState state = level.getBlockState(target);
			LivingForceKind targetKind = LivingForceKind.from(state);
			if (targetKind != null) {
				if (LivingForceRules.opposes(kind, targetKind)) requestClash(level, source);
				continue;
			}
			float destroySpeed = state.getDestroySpeed(level, target);
			if (!LivingForceRules.mayReplace(state.isAir(), !state.getFluidState().isEmpty(),
					level.getBlockEntity(target) != null, state.is(FORCE_SPREAD_IMMUNE), destroySpeed)) continue;
			level.setBlock(target, kind.block().defaultBlockState(), Block.UPDATE_ALL);
			register(level, target, kind);
			PhysicalMagicPresences.registerFixed(new MagicActionId(
					kind == LivingForceKind.DARKNESS ? "darkness_block" : "pure_light_block"),
					WORLD_MAGIC_OWNER, level, Vec3.atCenterOf(target), 1.5,
					level.getServer().getTickCount() + 200L, MagicPresenceHandle.Kind.FORCE_BLOCK);
			emitSpreadCue(level, source, target, kind, random);
		}
	}

	/** Advances affinity auras and every active, work-budgeted clash wave. */
	public static void tick(MinecraftServer server) {
		if (!LivingForceRules.mayAdvance(GlobalTimeStopManager.isStopped(server))) return;
		if (server.getTickCount() % 20 == 0) tickAuras(server);
		PowersConfig.LivingForces policy = PowersConfigLoader.get().livingForces();
		for (ServerLevel level : server.getAllLevels()) {
			List<ForceClashWave> waves = ACTIVE_CLASHES.get(level);
			if (waves == null) continue;
			waves.removeIf(wave -> wave.tick(policy.clashChecksPerTick()));
			if (waves.isEmpty()) ACTIVE_CLASHES.remove(level);
		}
	}

	/** Clears all server-level indexes during shutdown. */
	public static void clearAll() {
		INDEXES.values().forEach(LivingForceIndex::clear);
		INDEXES.clear();
		ACTIVE_CLASHES.clear();
	}

	/** Loaded force-block and active clash counts without scanning world state. */
	public static Diagnostics diagnostics() {
		int indexedBlocks = INDEXES.values().stream().mapToInt(LivingForceIndex::size).sum();
		int clashes = ACTIVE_CLASHES.values().stream().mapToInt(List::size).sum();
		long queries = INDEXES.values().stream().map(LivingForceIndex::diagnostics)
				.mapToLong(LivingForceIndex.Diagnostics::queries).sum();
		long candidates = INDEXES.values().stream().map(LivingForceIndex::diagnostics)
				.mapToLong(LivingForceIndex.Diagnostics::candidates).sum();
		long misses = INDEXES.values().stream().map(LivingForceIndex::diagnostics)
				.mapToLong(LivingForceIndex.Diagnostics::misses).sum();
		long stale = INDEXES.values().stream().map(LivingForceIndex::diagnostics)
				.mapToLong(LivingForceIndex.Diagnostics::staleRemovals).sum();
		long memory = INDEXES.values().stream().map(LivingForceIndex::diagnostics)
				.mapToLong(LivingForceIndex.Diagnostics::estimatedBytes).sum();
		return new Diagnostics(indexedBlocks, clashes, MAX_AURA_CANDIDATES_PER_LEVEL,
				MAX_AURA_CANDIDATES_PER_PLAYER, queries, candidates, misses, stale, memory);
	}

	public record Diagnostics(int indexedBlocks, int activeClashes,
			int auraCandidatesPerLevel, int auraCandidatesPerPlayer, long queries,
			long candidates, long misses, long staleRemovals, long estimatedBytes) {
	}

	/** Bounded loaded-only proximity query used by invasions and ceremonies. */
	public static boolean isNearForce(ServerLevel level, BlockPos center, int radius,
			LivingForceKind kind) {
		return isNearValidForce(level, index(level), Vec3.atCenterOf(center), radius, kind);
	}

	private static void registerLoaded(ServerLevel level, BlockPos pos, LivingForceKind kind) {
		if (kind != null) index(level).add(pos.asLong(), kind);
		if (kind != null) checkForClash(level, pos, kind);
	}

	private static void loadFrontier(ServerLevel level, LevelChunk chunk) {
		LivingForceFrontierSavedData saved = frontierData(level.getServer());
		String dimension = dimensionId(level);
		long chunkKey = chunk.getPos().pack();
		if (saved.hasChunk(dimension, chunkKey)) {
			Map<Long, LivingForceKind> repaired = new java.util.LinkedHashMap<>();
			for (Map.Entry<Long, LivingForceKind> entry : saved.frontier(dimension, chunkKey).entrySet()) {
				BlockPos position = BlockPos.of(entry.getKey());
				LivingForceKind actual = LivingForceKind.from(level.getBlockState(position));
				if (actual == null || !isFrontier(level, position, actual)) continue;
				repaired.put(entry.getKey(), actual);
				registerLoaded(level, position, actual);
			}
			saved.replaceChunk(dimension, chunkKey, repaired);
			return;
		}
		Map<Long, LivingForceKind> discovered = new java.util.LinkedHashMap<>();
		chunk.findBlocks(state -> LivingForceKind.from(state) != null, (position, state) -> {
			LivingForceKind kind = LivingForceKind.from(state);
			if (kind == null || !isFrontier(level, position, kind)) return;
			discovered.put(position.asLong(), kind);
			registerLoaded(level, position, kind);
		});
		saved.replaceChunk(dimension, chunkKey, discovered);
	}

	private static void refreshFrontier(ServerLevel level, BlockPos position) {
		if (!LoadedChunks.contains(level, position)) return;
		LivingForceKind kind = LivingForceKind.from(level.getBlockState(position));
		boolean frontier = kind != null && isFrontier(level, position, kind);
		if (frontier) index(level).add(position.asLong(), kind);
		else index(level).remove(position.asLong());
		LivingForceFrontierSavedData saved = frontierData(level.getServer());
		saved.update(dimensionId(level),
				net.minecraft.world.level.ChunkPos.pack(position.getX() >> 4, position.getZ() >> 4),
				position.asLong(), frontier ? kind : null);
		if (frontier) {
			for (Direction direction : Direction.values()) {
				BlockPos neighbor = position.relative(direction);
				if (LoadedChunks.contains(level, neighbor)) refreshNeighbor(level, neighbor, saved);
			}
		}
	}

	private static void refreshNeighbor(ServerLevel level, BlockPos position,
			LivingForceFrontierSavedData saved) {
		LivingForceKind kind = LivingForceKind.from(level.getBlockState(position));
		if (kind == null) return;
		boolean frontier = isFrontier(level, position, kind);
		if (frontier) index(level).add(position.asLong(), kind);
		else index(level).remove(position.asLong());
		saved.update(dimensionId(level),
				net.minecraft.world.level.ChunkPos.pack(position.getX() >> 4, position.getZ() >> 4),
				position.asLong(), frontier ? kind : null);
	}

	private static boolean isFrontier(ServerLevel level, BlockPos position, LivingForceKind kind) {
		for (Direction direction : Direction.values()) {
			BlockPos neighbor = position.relative(direction);
			if (!LoadedChunks.contains(level, neighbor)
					|| LivingForceKind.from(level.getBlockState(neighbor)) != kind) return true;
		}
		return false;
	}

	private static LivingForceFrontierSavedData frontierData(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(LivingForceFrontierSavedData.TYPE);
	}

	private static String dimensionId(ServerLevel level) {
		return level.dimension().identifier().toString();
	}

	private static void requestClash(ServerLevel level, BlockPos center) {
		PowersConfig.LivingForces policy = PowersConfigLoader.get().livingForces();
		List<ForceClashWave> waves = ACTIVE_CLASHES.computeIfAbsent(level, ignored -> new ArrayList<>());
		if (waves.stream().anyMatch(wave -> wave.overlaps(center))
				|| waves.size() >= MAX_ACTIVE_CLASHES_PER_LEVEL) return;
		waves.add(new ForceClashWave(level, center, policy.clashRadius()));
		Vec3 epicenter = Vec3.atCenterOf(center);
		PowerFx.forceClashDetonation(level, epicenter, policy.clashRadius());
		PowersMod.startStorm(level, epicenter, 24);
		damageClashEntities(level, epicenter, policy.clashRadius());
	}

	private static void damageClashEntities(ServerLevel level, Vec3 center, int radius) {
		AABB bounds = AABB.ofSize(center, radius * 2.0, radius * 2.0, radius * 2.0);
		List<LivingEntity> candidates = BoundedEntityCandidates.living(level, bounds,
				LivingForceRules.clashEntityInspectionLimit(), Entity::isAlive,
				Comparator.comparingDouble((LivingEntity entity) ->
						entity.position().distanceToSqr(center)).thenComparing(
						entity -> entity.getUUID().toString()));
		for (LivingEntity entity : candidates) {
			double distance = entity.position().distanceTo(center);
			double damage = LivingForceRules.clashDamage(distance, radius, PEAK_CLASH_DAMAGE);
			if (damage <= 0.0 || PowerProtection.isSafeZone(level, entity.position())) continue;
			entity.hurtServer(level, entity.damageSources().magic(), (float) damage);
			Vec3 direction = entity.position().subtract(center);
			if (direction.lengthSqr() < 1.0E-6) direction = new Vec3(0.0, 1.0, 0.0);
			else direction = direction.normalize();
			double impulse = LivingForceRules.clashImpulse(distance, radius, PEAK_CLASH_IMPULSE);
			entity.push(direction.x * impulse, Math.min(2.5, 0.25 + impulse * 0.3), direction.z * impulse);
		}
	}

	private static void tickAuras(MinecraftServer server) {
		PowersConfig.LivingForces policy = PowersConfigLoader.get().livingForces();
		for (ServerLevel level : server.getAllLevels()) {
			LivingForceIndex index = INDEXES.get(level);
			if (index == null || index.size() == 0) continue;
			Set<UUID> visited = new HashSet<>();
			ForceAuraWorkBudget budget = new ForceAuraWorkBudget(
					MAX_AURA_CANDIDATES_PER_LEVEL, MAX_AURA_CANDIDATES_PER_PLAYER);
			List<ServerPlayer> anchors = level.players().stream()
					.sorted(Comparator.comparing(player -> player.getUUID().toString()))
					.limit(MAX_AURA_PLAYERS_PER_LEVEL).toList();
			for (ServerPlayer anchor : anchors) {
				if (!budget.hasWork()) break;
				int allowance = budget.allowanceForPlayer();
				AABB bounds = anchor.getBoundingBox().inflate(ACTIVE_ENTITY_SCAN_RADIUS,
						Math.max(32.0, level.getMaxY() - level.getMinY()),
						ACTIVE_ENTITY_SCAN_RADIUS);
				BoundedEntityCandidates.Batch<LivingEntity> batch =
						BoundedEntityCandidates.collectBatch(level,
						net.minecraft.world.level.entity.EntityTypeTest.forClass(LivingEntity.class),
						bounds, allowance, Entity::isAlive);
				budget.recordInspections(batch.inspected());
				for (LivingEntity living : batch.candidates()) {
					if (!visited.add(living.getUUID())) continue;
					for (LivingForceKind kind : LivingForceKind.values()) {
						if (isNearValidForce(level, index, living, policy.auraRadius(), kind)) {
							applyForceAffinity(level, living, policy, kind);
						}
					}
				}
			}
		}
	}

	private static boolean isNearValidForce(ServerLevel level, LivingForceIndex index,
			LivingEntity entity, int radius, LivingForceKind kind) {
		Vec3 center = entity.position().add(0.0, entity.getBbHeight() * 0.5, 0.0);
		return isNearValidForce(level, index, center, radius, kind);
	}

	private static boolean isNearValidForce(ServerLevel level, LivingForceIndex index,
			Vec3 center, int radius, LivingForceKind kind) {
		for (long packed : index.within(center.x, center.y, center.z, radius, kind, 32)) {
			BlockPos pos = BlockPos.of(packed);
			if (!LoadedChunks.contains(level, pos)) continue;
			if (level.getBlockState(pos).is(kind.block())) return true;
			index.removeStale(packed);
		}
		return false;
	}

	private static void applyForceAffinity(ServerLevel level, LivingEntity entity,
			PowersConfig.LivingForces policy, LivingForceKind kind) {
		boolean darknessTagged = entity.entityTags().contains(SkillSystem.DARKNESS_TAG);
		LivingForceRules.Affinity affinity = LivingForceRules.affinity(darknessTagged, kind);
		Vec3 center = entity.position().add(0.0, entity.getBbHeight() * 0.55, 0.0);
		if (affinity == LivingForceRules.Affinity.WITHER) {
			if (PowerProtection.isSafeZone(level, entity.position())
					|| SpellFieldManager.isSanctuaryProtected(level, entity)) {
				PowerFx.rune(level, center, 0.7, 0x58C7FF, 8, level.getGameTime() * 0.08);
				return;
			}
			entity.addEffect(PowerStatusEffects.hidden(
					MobEffects.WITHER, 50, policy.witherAmplifier(), true, true));
			if (kind == LivingForceKind.DARKNESS) PowerFx.darknessAura(level, center, false);
			else {
				PowerFx.rune(level, center, 1.1, 0xFFF5C4, 14, level.getGameTime() * 0.1);
				PowerFx.coloredBurst(level, center, 0xFFFFFF, 7, 0.7);
			}
			return;
		}
		if (affinity == LivingForceRules.Affinity.RADIANCE) {
			entity.addEffect(PowerStatusEffects.hidden(MobEffects.REGENERATION, 50, 1, true, true));
			if (entity instanceof ServerPlayer player && !AmethystDampening.isDampened(player)) {
				PlayerPowers.PlayerPowersData data = PlayerPowers.get(player);
				if (data.regenerateEnergy(Math.max(1, policy.energyRefillPerSecond() / 2))) {
					PowersPackets.syncTo(player);
				}
			}
			PowerFx.rune(level, center, 1.15, 0xFFF5C4, 14, level.getGameTime() * 0.08);
			PowerFx.coloredBurst(level, center, 0xFFFFFF, 5, 0.55);
			return;
		}
		if (affinity != LivingForceRules.Affinity.REFILL) return;
		if (entity instanceof ServerPlayer player) {
			if (AmethystDampening.isDampened(player)) {
				PowerFx.amethystDarknessInterference(level, center);
				return;
			}
			PlayerPowers.PlayerPowersData data = PlayerPowers.get(player);
			int energyBefore = data.energy();
			int capacity = data.energyCapacity();
			boolean resurgence = PowerScalingService.hasVariant(player, "dark_resurgence");
			int rankedRefill = PowerScalingService.regeneration(player, policy.energyRefillPerSecond());
			int refill = RankVariantRules.darknessRefill(
					rankedRefill, energyBefore, capacity, resurgence);
			if (data.regenerateEnergy(refill)) {
				PowersPackets.syncTo(player);
				if (resurgence && RankVariantRules.darknessEmergency(energyBefore, capacity)) {
					PowerFx.darknessResurgence(level, center);
				}
			}
		}
		PowerFx.darknessAura(level, center, true);
	}

	private static LivingForceIndex index(ServerLevel level) {
		return INDEXES.computeIfAbsent(level, ignored -> new LivingForceIndex());
	}

	private static void emitSpreadCue(ServerLevel level, BlockPos source, BlockPos target,
			LivingForceKind kind, RandomSource random) {
		Vec3 origin = Vec3.atCenterOf(source);
		Vec3 center = Vec3.atCenterOf(target);
		int color = kind == LivingForceKind.DARKNESS ? 0x2A0C3D : 0xFFF4C7;
		PowerFx.beam(level, origin, center, PowerFx.dust(color, 1.0F), 6);
		PowerFx.coloredBurst(level, center, color, 5, 0.32);
		PowerFx.burst(level, center, kind == LivingForceKind.DARKNESS
				? PowersParticles.ECLIPSE : PowersParticles.MOTE, 3, 0.24, 0.015);
		PowerFx.rune(level, center.add(0.0, 0.03, 0.0), 0.42, color, 8,
				random.nextDouble() * Math.PI);
		if (random.nextInt(4) == 0) {
			PowerFx.sound(level, center, kind == LivingForceKind.DARKNESS
					? PowersSounds.DARK_WHISPER : PowersSounds.LIGHT_CHORUS, 0.35F, 0.72F);
		}
	}
}
