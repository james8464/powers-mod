package com.powers.magic.runtime;

import com.powers.magic.MagicActionId;
import com.powers.magic.InteractionContext;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

/**
 * Server-thread lifecycle bridge between abstract collision entries and actual
 * projectiles, entities, beams, impacts, fields, and living-force blocks.
 */
public final class PhysicalMagicPresences {
	private record Bound(MagicPresenceHandle handle, long expiresAt) {
	}

	private static final Map<MagicPresenceId, Bound> BOUND = new HashMap<>();
	private static final Map<UUID, MagicPresenceId> BY_ENTITY = new HashMap<>();
	private static final TreeMap<Long, Set<MagicPresenceId>> BY_EXPIRY = new TreeMap<>();
	private static final Map<CollisionKey, Long> COLLISION_COOLDOWNS = new HashMap<>();
	private static final int MAX_COLLISION_KEYS = 4_096;
	private static final int COLLISION_REPEAT_TICKS = 10;

	private record CollisionKey(MagicPresenceId first, MagicPresenceId second) {
		private CollisionKey {
			if (first.value().compareTo(second.value()) > 0) {
				MagicPresenceId swap = first;
				first = second;
				second = swap;
			}
		}
	}

	private PhysicalMagicPresences() {
	}

	/** Reanchors an already committed cast residue to a live entity or projectile. */
	public static MagicPresenceHandle bindExistingEntity(MagicPresenceId presenceId, Entity entity,
			MagicPresenceHandle.Kind kind, long expiresAt) {
		Objects.requireNonNull(entity, "entity");
		if (!(entity.level() instanceof ServerLevel level)) return null;
		MagicPresenceHandle handle = MagicPresenceHandle.entity(presenceId, kind, entity.getUUID());
		if (!MagicRuntime.global().rebindPresence(presenceId, dimension(level),
				anchor(kind, entity), expiresAt)) return null;
		put(handle, expiresAt);
		collideNearby(handle, level, entity.position(), level.getServer().getTickCount());
		return handle;
	}

	/** Reanchors a committed cast at an authoritative beam or impact point. */
	public static MagicPresenceHandle bindExistingFixed(MagicPresenceId presenceId, ServerLevel level,
			Vec3 point, MagicPresenceHandle.Kind kind, long expiresAt) {
		MagicPresenceHandle handle = MagicPresenceHandle.fixed(presenceId, kind);
		if (!MagicRuntime.global().rebindPresence(presenceId, dimension(level),
				PresenceAnchor.fixed(point.x, point.y, point.z), expiresAt)) return null;
		put(handle, expiresAt);
		collideNearby(handle, level, point, level.getServer().getTickCount());
		return handle;
	}

	/** Registers a physical field or force block that was not created by an ability residue. */
	public static MagicPresenceHandle registerFixed(MagicActionId action, UUID owner,
			ServerLevel level, Vec3 point, double radius, long expiresAt,
			MagicPresenceHandle.Kind kind) {
		MagicPresenceId id = MagicPresenceId.random();
		MagicRuntime.global().registerPresence(new MagicPresence(id, action, owner, dimension(level),
				PresenceAnchor.fixed(point.x, point.y, point.z), radius, expiresAt));
		MagicPresenceHandle handle = MagicPresenceHandle.fixed(id, kind);
		put(handle, expiresAt);
		collideNearby(handle, level, point, level.getServer().getTickCount());
		return handle;
	}

	/** Registers a body/entity presence for real body-field collision handling. */
	public static MagicPresenceHandle registerEntity(MagicActionId action, UUID owner,
			Entity entity, double radius, long expiresAt) {
		if (!(entity.level() instanceof ServerLevel level)) return null;
		MagicPresenceId id = MagicPresenceId.random();
		MagicRuntime.global().registerPresence(new MagicPresence(id, action, owner, dimension(level),
				anchor(MagicPresenceHandle.Kind.ENTITY, entity), radius, expiresAt));
		MagicPresenceHandle handle = MagicPresenceHandle.entity(id,
				MagicPresenceHandle.Kind.ENTITY, entity.getUUID());
		put(handle, expiresAt);
		collideNearby(handle, level, entity.position(), level.getServer().getTickCount());
		return handle;
	}

	/** Called from the entity tick mixin; work is O(1) for unbound vanilla entities. */
	public static void move(Entity entity) {
		MagicPresenceId id = BY_ENTITY.get(entity.getUUID());
		if (id == null || !(entity.level() instanceof ServerLevel level)) return;
		Bound bound = BOUND.get(id);
		if (bound == null) {
			BY_ENTITY.remove(entity.getUUID());
			return;
		}
		MagicRuntime.global().movePresence(id, dimension(level), anchor(bound.handle().kind(), entity));
		collideNearby(bound.handle(), level, entity.position(), level.getServer().getTickCount());
	}

	/** Converts a projectile presence into a short fixed impact without token duplication. */
	public static void fixEntity(Entity entity, ServerLevel level, Vec3 point,
			MagicPresenceHandle.Kind kind, long expiresAt) {
		MagicPresenceId id = BY_ENTITY.remove(entity.getUUID());
		if (id == null) return;
		bindExistingFixed(id, level, point, kind, expiresAt);
	}

	/** Ends a bound presence when the authoritative entity unloads or is removed. */
	public static void unload(Entity entity) {
		MagicPresenceId id = BY_ENTITY.remove(entity.getUUID());
		if (id == null) return;
		Bound removed = BOUND.remove(id);
		if (removed != null) unscheduleExpiry(id, removed.expiresAt());
		MagicRuntime.global().removePresence(id);
	}

	public static void remove(MagicPresenceHandle handle) {
		if (handle == null) return;
		Bound removed = BOUND.remove(handle.presenceId());
		if (removed != null && removed.handle().boundEntity() != null) {
			BY_ENTITY.remove(removed.handle().boundEntity(), handle.presenceId());
		}
		if (removed != null) unscheduleExpiry(handle.presenceId(), removed.expiresAt());
		MagicRuntime.global().removePresence(handle.presenceId());
	}

	/** Prunes only handle buckets whose authoritative world-time expiry has elapsed. */
	public static void tick(long gameTime) {
		if (gameTime < 0L) throw new IllegalArgumentException("Game time cannot be negative");
		for (MagicPresenceId id : BY_EXPIRY.headMap(gameTime, true).values().stream()
				.flatMap(Collection::stream).toList()) {
			Bound removed = BOUND.remove(id);
			if (removed == null) continue;
			UUID entityId = removed.handle().boundEntity();
			if (entityId != null) BY_ENTITY.remove(entityId, id);
			unscheduleExpiry(id, removed.expiresAt());
		}
	}

	public static int activeHandleCount() {
		return BOUND.size();
	}

	public static void clear() {
		BOUND.clear();
		BY_ENTITY.clear();
		BY_EXPIRY.clear();
		COLLISION_COOLDOWNS.clear();
	}

	/** Resolves every indexed physical overlap once per bounded repeat window. */
	public static int collideNearby(MagicPresenceHandle handle, ServerLevel level,
			Vec3 point, long gameTime) {
		if (handle == null || level == null || point == null) return 0;
		MagicRuntime runtime = MagicRuntime.global();
		MagicPresence first = runtime.presence(handle.presenceId());
		if (first == null) return 0;
		COLLISION_COOLDOWNS.entrySet().removeIf(entry -> entry.getValue() <= gameTime);
		int resolved = 0;
		for (MagicPresence second : runtime.nearbyPhysical(first, Math.max(2.0, first.radius()), gameTime)
				.stream().sorted(java.util.Comparator.comparing(presence -> presence.id().value())).toList()) {
			if (second.id().equals(first.id())) continue;
			Bound secondBound = BOUND.get(second.id());
			if (secondBound == null || PhysicalCollisionFamily.of(handle.kind(),
					secondBound.handle().kind()) == PhysicalCollisionFamily.UNSUPPORTED) continue;
			CollisionKey key = new CollisionKey(first.id(), second.id());
			if (COLLISION_COOLDOWNS.getOrDefault(key, Long.MIN_VALUE) > gameTime) continue;
			if (COLLISION_COOLDOWNS.size() >= MAX_COLLISION_KEYS) {
				COLLISION_COOLDOWNS.remove(COLLISION_COOLDOWNS.keySet().iterator().next());
			}
			COLLISION_COOLDOWNS.put(key, gameTime + COLLISION_REPEAT_TICKS);
			var definition = MagicRuntime.catalogue().definition(first.action());
			var resolution = runtime.resolveInteraction(first.action().value(), second.action().value());
			MagicCastContext cast = new MagicCastContext(definition, first.owner(), first.dimension(),
					first.anchor(), Math.max(1.0, first.radius()), gameTime, InteractionContext.DEFAULT);
			ServerMagicCasts.emitPhysicalPresenceReaction(level,
					new MagicReactionEvent(cast, second, resolution));
			if (resolution.blocksFirst()) removeTransient(handle, level);
			if (resolution.blocksSecond()) removeTransient(secondBound.handle(), level);
			resolved++;
			if (runtime.presence(first.id()) == null) break;
		}
		return resolved;
	}

	private static void removeTransient(MagicPresenceHandle handle, ServerLevel level) {
		if (handle.kind() != MagicPresenceHandle.Kind.PROJECTILE
				&& handle.kind() != MagicPresenceHandle.Kind.BEAM
				&& handle.kind() != MagicPresenceHandle.Kind.IMPACT) return;
		if (handle.boundEntity() != null) {
			Entity entity = level.getEntity(handle.boundEntity());
			if (entity != null) entity.discard();
		}
		remove(handle);
	}

	private static void put(MagicPresenceHandle handle, long expiresAt) {
		Bound previous = BOUND.put(handle.presenceId(), new Bound(handle, expiresAt));
		if (previous != null && previous.handle().boundEntity() != null) {
			BY_ENTITY.remove(previous.handle().boundEntity(), handle.presenceId());
		}
		if (previous != null) unscheduleExpiry(handle.presenceId(), previous.expiresAt());
		if (handle.boundEntity() != null) BY_ENTITY.put(handle.boundEntity(), handle.presenceId());
		BY_EXPIRY.computeIfAbsent(expiresAt, ignored -> new LinkedHashSet<>()).add(handle.presenceId());
	}

	private static void unscheduleExpiry(MagicPresenceId id, long expiresAt) {
		Set<MagicPresenceId> bucket = BY_EXPIRY.get(expiresAt);
		if (bucket == null) return;
		bucket.remove(id);
		if (bucket.isEmpty()) BY_EXPIRY.remove(expiresAt);
	}

	private static PresenceAnchor anchor(MagicPresenceHandle.Kind kind, Entity entity) {
		PresenceAnchor.Kind anchorKind = kind == MagicPresenceHandle.Kind.PROJECTILE
				? PresenceAnchor.Kind.PROJECTILE : PresenceAnchor.Kind.ENTITY;
		return PresenceAnchor.entity(anchorKind, entity.getUUID(),
				entity.getX(), entity.getY(), entity.getZ());
	}

	private static String dimension(ServerLevel level) {
		return level.dimension().identifier().toString();
	}
}
