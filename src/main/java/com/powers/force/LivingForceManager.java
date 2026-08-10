package com.powers.force;

import com.powers.PowersMod;
import com.powers.PowerStatusEffects;
import com.powers.PowersParticles;
import com.powers.PowersSounds;
import com.powers.player.PlayerPowers;
import com.powers.player.SkillSystem;
import com.powers.power.AmethystDampening;
import com.powers.progression.PowerScalingService;
import com.powers.progression.RankVariantRules;
import com.powers.config.PowersConfig;
import com.powers.config.PowersConfigLoader;
import com.powers.fx.PowerFx;
import com.powers.protection.PowerProtection;
import com.powers.network.PowersPackets;
import com.powers.util.BoundedEntityCandidates;
import com.powers.util.LoadedChunks;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
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
	private static final int MAX_AURA_CANDIDATES_PER_CHUNK = 256;
	private static final int MAX_AURA_CANDIDATES_PER_LEVEL = 4_096;

	private LivingForceManager() {
	}

	/** Registers chunk lifecycle hooks that rebuild and evict the spatial index. */
	public static void initialize() {
		ServerChunkEvents.CHUNK_LOAD.register((level, chunk, newlyGenerated) ->
				chunk.findBlocks(state -> LivingForceKind.from(state) != null,
						(pos, state) -> registerLoaded(level, pos, LivingForceKind.from(state))));
		ServerChunkEvents.CHUNK_UNLOAD.register((level, chunk) ->
				index(level).removeChunk(chunk.getPos().pack()));
	}

	static void register(ServerLevel level, BlockPos pos, LivingForceKind kind) {
		if (kind != null) index(level).add(pos.asLong(), kind);
	}

	static void unregister(ServerLevel level, BlockPos pos) {
		index(level).remove(pos.asLong());
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
			emitSpreadCue(level, source, target, kind, random);
		}
	}

	/** Advances affinity auras and every active, work-budgeted clash wave. */
	public static void tick(MinecraftServer server) {
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

	private static void registerLoaded(ServerLevel level, BlockPos pos, LivingForceKind kind) {
		register(level, pos, kind);
		if (kind != null) checkForClash(level, pos, kind);
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
			for (long packedChunk : index.chunksWith(LivingForceKind.DARKNESS)) {
				if (visited.size() >= MAX_AURA_CANDIDATES_PER_LEVEL) break;
				int chunkX = (int) packedChunk;
				int chunkZ = (int) (packedChunk >>> 32);
				double radius = policy.auraRadius();
				AABB bounds = new AABB(chunkX * 16.0 - radius, level.getMinY(),
						chunkZ * 16.0 - radius, chunkX * 16.0 + 16.0 + radius,
						level.getMaxY(), chunkZ * 16.0 + 16.0 + radius);
				for (LivingEntity living : BoundedEntityCandidates.living(level, bounds,
						MAX_AURA_CANDIDATES_PER_CHUNK, Entity::isAlive)) {
					if (!visited.add(living.getUUID())) continue;
					if (isNearValidDarkness(level, index, living, policy.auraRadius())) {
						applyDarknessAffinity(level, living, policy);
					}
				}
			}
		}
	}

	private static boolean isNearValidDarkness(ServerLevel level, LivingForceIndex index,
			LivingEntity entity, int radius) {
		Vec3 center = entity.position().add(0.0, entity.getBbHeight() * 0.5, 0.0);
		for (long packed : index.within(center.x, center.y, center.z, radius, LivingForceKind.DARKNESS)) {
			BlockPos pos = BlockPos.of(packed);
			if (!LoadedChunks.contains(level, pos)) continue;
			if (level.getBlockState(pos).is(LivingForceKind.DARKNESS.block())) return true;
			index.remove(packed);
		}
		return false;
	}

	private static void applyDarknessAffinity(ServerLevel level, LivingEntity entity,
			PowersConfig.LivingForces policy) {
		boolean darknessTagged = entity.entityTags().contains(SkillSystem.DARKNESS_TAG);
		LivingForceRules.Affinity affinity = LivingForceRules.affinity(darknessTagged, LivingForceKind.DARKNESS);
		Vec3 center = entity.position().add(0.0, entity.getBbHeight() * 0.55, 0.0);
		if (affinity == LivingForceRules.Affinity.WITHER) {
			if (PowerProtection.isSafeZone(level, entity.position())) {
				PowerFx.rune(level, center, 0.7, 0x58C7FF, 8, level.getGameTime() * 0.08);
				return;
			}
			entity.addEffect(PowerStatusEffects.hidden(
					MobEffects.WITHER, 50, policy.witherAmplifier(), true, true));
			PowerFx.darknessAura(level, center, false);
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
		PowerFx.beam(level, origin, center,
				ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT,
						0xFF000000 | color), 6);
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
