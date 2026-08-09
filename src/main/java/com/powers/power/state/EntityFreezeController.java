package com.powers.power.state;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Shared, reference-counted freeze state for every time-based power. */
public final class EntityFreezeController {
	private record Frozen(Entity entity, Vec3 position, Vec3 velocity, boolean noGravity,
			boolean noAi, double fallDistance) {
	}

	private static final Map<UUID, Frozen> SAVED = new HashMap<>();
	private static final OwnedFreezeIndex OWNERS = new OwnedFreezeIndex();

	private EntityFreezeController() {
	}

	public static void claim(Entity entity, UUID owner) {
		UUID id = entity.getUUID();
		if (OWNERS.claim(id, owner)) {
			SAVED.put(id, new Frozen(entity, entity.position(), entity.getDeltaMovement(), entity.isNoGravity(),
					entity instanceof Mob mob && mob.isNoAi(), entity.fallDistance));
		}
	}

	public static void release(UUID owner, Collection<UUID> entities) {
		for (UUID entityId : entities) {
			if (!OWNERS.release(entityId, owner)) continue;
			restore(SAVED.remove(entityId));
		}
	}

	public static void holdAll() {
		for (Frozen frozen : SAVED.values()) {
			Entity entity = frozen.entity();
			if (entity.isRemoved()) continue;
			entity.setDeltaMovement(Vec3.ZERO);
			entity.setNoGravity(true);
			entity.setPos(frozen.position().x, frozen.position().y, frozen.position().z);
			if (entity instanceof Mob mob) mob.setNoAi(true);
		}
	}

	public static boolean isFrozen(Entity entity) {
		return OWNERS.isClaimed(entity.getUUID());
	}

	/**
	 * Replaces a frozen entity's eventual release velocity with rest. This lets a
	 * movement field relinquish ownership without a time power later restoring
	 * stale launch momentum captured on the preceding tick.
	 */
	public static boolean neutralizeReleaseMotion(Entity entity) {
		if (entity == null) return false;
		Frozen frozen = SAVED.get(entity.getUUID());
		if (frozen == null) return false;
		SAVED.put(entity.getUUID(), new Frozen(frozen.entity(), frozen.position(), Vec3.ZERO,
				frozen.noGravity(), frozen.noAi(), 0.0));
		return true;
	}

	public static void clearAll() {
		for (Frozen frozen : SAVED.values()) restore(frozen);
		SAVED.clear();
		OWNERS.clear();
	}

	private static void restore(Frozen frozen) {
		if (frozen == null || frozen.entity().isRemoved()) return;
		Entity entity = frozen.entity();
		entity.setNoGravity(frozen.noGravity());
		entity.setDeltaMovement(frozen.velocity());
		entity.fallDistance = frozen.fallDistance();
		if (entity instanceof Mob mob) mob.setNoAi(frozen.noAi());
	}
}
