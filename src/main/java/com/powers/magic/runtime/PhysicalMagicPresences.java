package com.powers.magic.runtime;

import com.powers.magic.MagicActionId;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
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
		return handle;
	}

	/** Reanchors a committed cast at an authoritative beam or impact point. */
	public static MagicPresenceHandle bindExistingFixed(MagicPresenceId presenceId, ServerLevel level,
			Vec3 point, MagicPresenceHandle.Kind kind, long expiresAt) {
		MagicPresenceHandle handle = MagicPresenceHandle.fixed(presenceId, kind);
		if (!MagicRuntime.global().rebindPresence(presenceId, dimension(level),
				PresenceAnchor.fixed(point.x, point.y, point.z), expiresAt)) return null;
		put(handle, expiresAt);
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
		BOUND.remove(id);
		MagicRuntime.global().removePresence(id);
	}

	public static void remove(MagicPresenceHandle handle) {
		if (handle == null) return;
		Bound removed = BOUND.remove(handle.presenceId());
		if (removed != null && removed.handle().boundEntity() != null) {
			BY_ENTITY.remove(removed.handle().boundEntity(), handle.presenceId());
		}
		MagicRuntime.global().removePresence(handle.presenceId());
	}

	/** Prunes handle metadata after the runtime's matching expiry pass. */
	public static void tick(long gameTime) {
		Iterator<Map.Entry<MagicPresenceId, Bound>> iterator = BOUND.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<MagicPresenceId, Bound> entry = iterator.next();
			if (entry.getValue().expiresAt() > gameTime) continue;
			UUID entityId = entry.getValue().handle().boundEntity();
			if (entityId != null) BY_ENTITY.remove(entityId, entry.getKey());
			iterator.remove();
		}
	}

	public static int activeHandleCount() {
		return BOUND.size();
	}

	public static void clear() {
		BOUND.clear();
		BY_ENTITY.clear();
	}

	private static void put(MagicPresenceHandle handle, long expiresAt) {
		Bound previous = BOUND.put(handle.presenceId(), new Bound(handle, expiresAt));
		if (previous != null && previous.handle().boundEntity() != null) {
			BY_ENTITY.remove(previous.handle().boundEntity(), handle.presenceId());
		}
		if (handle.boundEntity() != null) BY_ENTITY.put(handle.boundEntity(), handle.presenceId());
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
