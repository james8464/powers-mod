package com.powers.force;

import com.powers.PowersMod;
import com.powers.PowersParticles;
import com.powers.PowersSounds;
import com.powers.config.PowersConfig;
import com.powers.config.PowersConfigLoader;
import com.powers.fx.PowerFx;
import com.powers.protection.PowerProtection;
import com.powers.util.LoadedChunks;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.WeakHashMap;

/** Owns loaded-force indexing and bounded server-side terrain spreading. */
public final class LivingForceManager {
	/** Blocks a datapack may protect from either living force. */
	public static final TagKey<Block> FORCE_SPREAD_IMMUNE =
			TagKey.create(Registries.BLOCK, PowersMod.id("living_force_immune"));

	private static final Map<ServerLevel, LivingForceIndex> INDEXES = new WeakHashMap<>();

	private LivingForceManager() {
	}

	/** Registers chunk lifecycle hooks that rebuild and evict the spatial index. */
	public static void initialize() {
		ServerChunkEvents.CHUNK_LOAD.register((level, chunk, newlyGenerated) ->
				chunk.findBlocks(state -> LivingForceKind.from(state) != null,
						(pos, state) -> register(level, pos, LivingForceKind.from(state))));
		ServerChunkEvents.CHUNK_UNLOAD.register((level, chunk) ->
				index(level).removeChunk(chunk.getPos().pack()));
	}

	static void register(ServerLevel level, BlockPos pos, LivingForceKind kind) {
		if (kind != null) index(level).add(pos.asLong(), kind);
	}

	static void unregister(ServerLevel level, BlockPos pos) {
		index(level).remove(pos.asLong());
	}

	/** Makes a few face-adjacent conversion attempts when vanilla selects a force block for a random tick. */
	static void spread(ServerLevel level, BlockPos source, LivingForceKind kind, RandomSource random) {
		register(level, source, kind);
		PowersConfig.LivingForces policy = PowersConfigLoader.get().livingForces();
		if (!policy.spreadingEnabled() || PowerProtection.isSafeZone(level, Vec3.atCenterOf(source))) return;
		for (int attempt = 0; attempt < policy.spreadAttempts(); attempt++) {
			BlockPos target = source.relative(Direction.getRandom(random));
			if (!LoadedChunks.contains(level, target)
					|| PowerProtection.isSafeZone(level, Vec3.atCenterOf(target))) continue;
			BlockState state = level.getBlockState(target);
			if (LivingForceKind.from(state) != null) continue;
			float destroySpeed = state.getDestroySpeed(level, target);
			if (!LivingForceRules.mayReplace(state.isAir(), !state.getFluidState().isEmpty(),
					level.getBlockEntity(target) != null, state.is(FORCE_SPREAD_IMMUNE), destroySpeed)) continue;
			level.setBlock(target, kind.block().defaultBlockState(), Block.UPDATE_ALL);
			register(level, target, kind);
			emitSpreadCue(level, target, kind, random);
		}
	}

	/** Clears all server-level indexes during shutdown. */
	public static void clearAll() {
		INDEXES.values().forEach(LivingForceIndex::clear);
		INDEXES.clear();
	}

	private static LivingForceIndex index(ServerLevel level) {
		return INDEXES.computeIfAbsent(level, ignored -> new LivingForceIndex());
	}

	private static void emitSpreadCue(ServerLevel level, BlockPos target, LivingForceKind kind,
			RandomSource random) {
		Vec3 center = Vec3.atCenterOf(target);
		int color = kind == LivingForceKind.DARKNESS ? 0x2A0C3D : 0xFFF4C7;
		PowerFx.coloredBurst(level, center, color, 5, 0.32);
		PowerFx.burst(level, center, kind == LivingForceKind.DARKNESS
				? PowersParticles.ECLIPSE : PowersParticles.MOTE, 3, 0.24, 0.015);
		if (random.nextInt(4) == 0) {
			PowerFx.sound(level, center, kind == LivingForceKind.DARKNESS
					? PowersSounds.DARK_WHISPER : PowersSounds.LIGHT_CHORUS, 0.35F, 0.72F);
		}
	}
}
