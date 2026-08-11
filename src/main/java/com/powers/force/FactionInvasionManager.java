package com.powers.force;

import com.powers.PowersEntities;
import com.powers.PowersSounds;
import com.powers.entity.AbstractPlayerLikeMob;
import com.powers.fx.PowerFx;
import com.powers.player.SkillSystem;
import com.powers.protection.PowerProtection;
import com.powers.util.BoundedEntityCandidates;
import com.powers.util.LoadedChunks;
import com.powers.util.PowerMessages;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/** Manifests a small, finite opposing patrol at active living-force scars. */
public final class FactionInvasionManager {
	private static final int PULSE_TICKS = 200;
	private static final int MAX_PLAYER_ANCHORS = 64;
	private static final Map<UUID, ResourceKey<Level>> ACTIVE = new HashMap<>();

	private FactionInvasionManager() {
	}

	public static void tick(MinecraftServer server) {
		if (server.getTickCount() % PULSE_TICKS != 0) return;
		prune(server);
		if (ACTIVE.size() >= FactionInvasionRules.GLOBAL_INVADER_CAP) return;
		int anchors = 0;
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (anchors++ >= MAX_PLAYER_ANCHORS
					|| PowerProtection.isSafeZone((ServerLevel) player.level(), player.position())) continue;
			ServerLevel level = (ServerLevel) player.level();
			LivingForceKind force = opposingForce(level, player);
			if (force == null || nearbyInvaders(level, player) >= FactionInvasionRules.NEARBY_INVADER_CAP) {
				continue;
			}
			spawn(level, player, force, server.getTickCount());
			if (ACTIVE.size() >= FactionInvasionRules.GLOBAL_INVADER_CAP) break;
		}
	}

	private static LivingForceKind opposingForce(ServerLevel level, ServerPlayer player) {
		boolean darkness = SkillSystem.hasDarknessTag(player);
		for (LivingForceKind kind : LivingForceKind.values()) {
			if (FactionInvasionRules.shouldInvade(kind, darkness)
					&& LivingForceManager.isNearForce(level, player.blockPosition(), 12, kind)) return kind;
		}
		return null;
	}

	private static int nearbyInvaders(ServerLevel level, ServerPlayer player) {
		AABB bounds = player.getBoundingBox().inflate(32.0);
		return BoundedEntityCandidates.ofClass(level, AbstractPlayerLikeMob.class, bounds,
				FactionInvasionRules.NEARBY_INVADER_CAP, guardian -> guardian.isAlive()
						&& guardian.temporaryGuardian() && guardian.guardianOwner() == null).size();
	}

	private static void spawn(ServerLevel level, ServerPlayer target, LivingForceKind force, int tick) {
		BlockPos spawn = findSpawn(level, target.blockPosition(), target.getUUID().hashCode() ^ tick);
		if (spawn == null) return;
		AbstractPlayerLikeMob guardian = force == LivingForceKind.DARKNESS
				? PowersEntities.DARKNESS_CREATURE.create(level, EntitySpawnReason.EVENT)
				: PowersEntities.RADIANT_SENTINEL.create(level, EntitySpawnReason.EVENT);
		if (guardian == null) return;
		guardian.setPos(spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5);
		guardian.finalizeSpawn(level, level.getCurrentDifficultyAt(spawn),
				EntitySpawnReason.EVENT, null);
		guardian.configureGuardian(null, FactionInvasionRules.INVADER_LIFETIME_TICKS, false);
		guardian.setTarget(target);
		if (!level.addFreshEntity(guardian)) return;
		ACTIVE.put(guardian.getUUID(), level.dimension());
		manifestScar(level, spawn.below(), force);
		var bolt = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
		if (bolt != null) {
			bolt.setVisualOnly(true);
			bolt.setPos(guardian.position());
			level.addFreshEntity(bolt);
		}
		int color = force == LivingForceKind.DARKNESS ? 0x351047 : 0xFFF1B8;
		PowerFx.rune(level, guardian.position(), 2.2, color, 28, 0.0);
		PowerFx.spiral(level, guardian.position(), 1.0, 4.0, color, 30, 0.0);
		PowerFx.sound(level, guardian.position(), force == LivingForceKind.DARKNESS
				? PowersSounds.DARK_WHISPER : PowersSounds.LIGHT_CHORUS, 1.6F,
				force == LivingForceKind.DARKNESS ? 0.45F : 1.4F);
		PowerMessages.send(target, force == LivingForceKind.DARKNESS
				? "force.powers.dark_invasion" : "force.powers.light_invasion", 1);
	}

	private static BlockPos findSpawn(ServerLevel level, BlockPos center, int seed) {
		for (int attempt = 0; attempt < 8; attempt++) {
			double angle = ((seed & 0xFFFF) / 65535.0 + attempt / 8.0) * Math.PI * 2.0;
			int x = center.getX() + (int) Math.round(Math.cos(angle) * (7 + attempt % 3));
			int z = center.getZ() + (int) Math.round(Math.sin(angle) * (7 + attempt % 3));
			BlockPos probe = new BlockPos(x, center.getY(), z);
			if (!LoadedChunks.contains(level, probe)) continue;
			int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
			BlockPos candidate = new BlockPos(x, y, z);
			if (LoadedChunks.contains(level, candidate) && level.getBlockState(candidate).isAir()
					&& level.getBlockState(candidate.above()).isAir()) return candidate;
		}
		return null;
	}

	private static void manifestScar(ServerLevel level, BlockPos center, LivingForceKind kind) {
		for (FactionInvasionRules.Offset offset : FactionInvasionRules.scarOffsets()) {
			BlockPos target = center.offset(offset.x(), 0, offset.z());
			if (!LoadedChunks.contains(level, target)
					|| PowerProtection.isSafeZone(level, Vec3.atCenterOf(target))) continue;
			var state = level.getBlockState(target);
			if (LivingForceKind.from(state) != null) continue;
			float speed = state.getDestroySpeed(level, target);
			if (!LivingForceRules.mayReplace(state.isAir(), !state.getFluidState().isEmpty(),
					level.getBlockEntity(target) != null, state.is(LivingForceManager.FORCE_SPREAD_IMMUNE),
					speed)) continue;
			level.setBlock(target, kind.block().defaultBlockState(), Block.UPDATE_ALL);
			LivingForceManager.register(level, target, kind);
		}
		PowerFx.burst(level, Vec3.atCenterOf(center), com.powers.PowersParticles.ECLIPSE, 8, 1.2, 0.02);
	}

	private static void prune(MinecraftServer server) {
		Iterator<Map.Entry<UUID, ResourceKey<Level>>> iterator = ACTIVE.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<UUID, ResourceKey<Level>> entry = iterator.next();
			ServerLevel level = server.getLevel(entry.getValue());
			if (level == null || level.getEntity(entry.getKey()) == null) iterator.remove();
		}
	}

	public static Diagnostics diagnostics() {
		return new Diagnostics(ACTIVE.size(), FactionInvasionRules.GLOBAL_INVADER_CAP);
	}

	public static void clear() {
		ACTIVE.clear();
	}

	public record Diagnostics(int activeInvaders, int globalCap) {
	}
}
