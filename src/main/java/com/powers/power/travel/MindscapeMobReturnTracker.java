package com.powers.power.travel;

import com.powers.companion.ShadowCompanionEntity;
import com.powers.util.LoadedChunks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Bounded origins for ordinary mobs carried into a mindscape by a crystal. */
public final class MindscapeMobReturnTracker {
	public static final int MAX_TRACKED = 256;

	public enum TrackDecision {
		TRACK,
		SKIP_DEAD,
		SKIP_PLAYER,
		SKIP_SHADOW
	}

	private record Origin(UUID journeyOwner, ResourceKey<Level> dimension,
			Vec3 position, float yRot, float xRot) {
	}

	private static final Map<UUID, Origin> ORIGINS = new LinkedHashMap<>();

	private MindscapeMobReturnTracker() {
	}

	public static TrackDecision trackDecision(boolean alive, boolean player, boolean shadow) {
		if (!alive) return TrackDecision.SKIP_DEAD;
		if (player) return TrackDecision.SKIP_PLAYER;
		if (shadow) return TrackDecision.SKIP_SHADOW;
		return TrackDecision.TRACK;
	}

	public static boolean track(UUID journeyOwner, LivingEntity entity) {
		if (entity == null || trackDecision(entity.isAlive() && !entity.isRemoved(),
				entity instanceof ServerPlayer, entity instanceof ShadowCompanionEntity) != TrackDecision.TRACK
				|| journeyOwner == null) {
			return false;
		}
		while (ORIGINS.size() >= MAX_TRACKED) {
			UUID eldest = ORIGINS.keySet().iterator().next();
			ORIGINS.remove(eldest);
		}
		ORIGINS.put(entity.getUUID(), new Origin(journeyOwner, entity.level().dimension(), entity.position(),
				entity.getYRot(), entity.getXRot()));
		return true;
	}

	/** Returns every ordinary mob carried by this caster's current mindscape journey. */
	public static int returnOwned(MinecraftServer server, UUID journeyOwner) {
		if (journeyOwner == null) return 0;
		java.util.List<UUID> owned = ORIGINS.entrySet().stream()
				.filter(entry -> journeyOwner.equals(entry.getValue().journeyOwner()))
				.map(Map.Entry::getKey)
				.toList();
		int requested = 0;
		for (UUID entityId : owned) {
			Origin origin = ORIGINS.get(entityId);
			if (origin == null) continue;
			LivingEntity current = find(server, entityId);
			if (current != null && returnToOrigin(current)) requested++;
		}
		return requested;
	}

	public static boolean tracked(LivingEntity entity) {
		return entity != null && ORIGINS.containsKey(entity.getUUID());
	}

	/** Returns immediately when loaded, otherwise owns one bounded asynchronous chunk request. */
	public static boolean returnToOrigin(LivingEntity entity) {
		if (entity == null) return false;
		UUID entityId = entity.getUUID();
		Origin origin = ORIGINS.get(entityId);
		if (origin == null) return false;
		// Cross-dimensional teleports replace non-player entity instances. Resolve the
		// live instance before both the immediate and asynchronously loaded paths so a
		// rollback cannot discard the origin merely because its caller held the old body.
		MinecraftServer server = entity.level().getServer();
		ServerLevel originLevel = server.getLevel(origin.dimension());
		if (originLevel == null) return false;
		LivingEntity current = find(server, entityId);
		if (current == null) {
			ORIGINS.remove(entityId);
			return false;
		}
		BlockPos block = BlockPos.containing(origin.position());
		if (LoadedChunks.contains(originLevel, block)) return complete(current, origin, originLevel);
		return TravelChunkLoader.request(entityId, originLevel, block, "mindscape_mob_return",
				MindscapeMobReturnTracker::completeLoaded, (currentServer, owner) -> { });
	}

	public static void forget(LivingEntity entity) {
		if (entity != null) ORIGINS.remove(entity.getUUID());
	}

	public static int trackedCount() {
		return ORIGINS.size();
	}

	/** Number of loaded or pending mob-return records owned by one realm traveller. */
	public static int trackedCount(UUID journeyOwner) {
		if (journeyOwner == null) return 0;
		return (int) ORIGINS.values().stream()
				.filter(origin -> journeyOwner.equals(origin.journeyOwner()))
				.count();
	}

	public static void clear() {
		ORIGINS.clear();
	}

	private static void completeLoaded(MinecraftServer server, UUID entityId) {
		Origin origin = ORIGINS.get(entityId);
		ServerLevel originLevel = origin == null ? null : server.getLevel(origin.dimension());
		LivingEntity entity = find(server, entityId);
		if (origin != null && originLevel != null && entity != null) {
			complete(entity, origin, originLevel);
		}
	}

	private static boolean complete(LivingEntity entity, Origin origin, ServerLevel originLevel) {
		if (!entity.isAlive() || entity.isRemoved()) {
			ORIGINS.remove(entity.getUUID());
			return false;
		}
		Entity moved = entity.teleport(new TeleportTransition(originLevel, origin.position(), Vec3.ZERO,
				origin.yRot(), origin.xRot(), TeleportTransition.PLAY_PORTAL_SOUND));
		if (moved == null) return false;
		ORIGINS.remove(entity.getUUID());
		return true;
	}

	private static LivingEntity find(MinecraftServer server, UUID entityId) {
		for (ServerLevel level : server.getAllLevels()) {
			Entity entity = level.getEntity(entityId);
			if (entity instanceof LivingEntity living) return living;
		}
		return null;
	}
}
