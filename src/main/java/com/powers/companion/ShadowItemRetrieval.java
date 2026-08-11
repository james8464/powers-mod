package com.powers.companion;

import com.powers.util.BoundedEntityCandidates;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;

/** Retrieves only legitimate loaded dropped items through a hard-capped local query. */
public final class ShadowItemRetrieval {
	public static final double RADIUS = 32.0;
	public static final int MAX_CANDIDATES = 64;
	public static final int MAX_TASK_TICKS = 200;

	private ShadowItemRetrieval() {
	}

	public static Optional<ItemEntity> find(ServerLevel level, Vec3 origin, Item item,
			int count, UUID requester) {
		AABB bounds = AABB.ofSize(origin, RADIUS * 2.0, RADIUS * 2.0, RADIUS * 2.0);
		return BoundedEntityCandidates.ofClass(level, ItemEntity.class, bounds, MAX_CANDIDATES,
				candidate -> candidate.isAlive() && candidate.getItem().is(item)
						&& candidate.getItem().getCount() >= Math.max(1, count)
						&& (candidate.getOwner() == null
						|| !candidate.getOwner().getUUID().equals(requester))).stream()
				.min(Comparator.comparingDouble(candidate -> candidate.distanceToSqr(origin)));
	}

	public static boolean deliver(ServerPlayer owner, ItemEntity source, int requested) {
		int count = Math.min(Math.max(1, requested), source.getItem().getCount());
		ItemStack delivery = source.getItem().copyWithCount(count);
		if (!owner.addItem(delivery) && !delivery.isEmpty()) owner.drop(delivery, false);
		source.getItem().shrink(count);
		if (source.getItem().isEmpty()) source.discard();
		return true;
	}
}
