package com.powers.power;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Line-of-sight targeting shared by every ability that needs to aim at a
 * creature. {@code Entity.pick} only clips blocks, so entity hits are found
 * with a proper raycast and combined with the block clip to respect walls.
 */
public final class PowerTargeting {
	private PowerTargeting() {
	}

	/**
	 * The nearest hit along the player's look line: either a block clipped by
	 * {@code Entity.pick} or an entity found by an entity raycast, whichever
	 * is closer. Returns a miss when nothing is hit.
	 */
	public static HitResult raycast(ServerPlayer player, double range) {
		HitResult block = player.pick(range, 0.0F, false);
		Vec3 start = player.getEyePosition();
		Vec3 end = start.add(player.getLookAngle().scale(range));
		EntityHitResult entity = ProjectileUtil.getEntityHitResult(
				player, start, end,
				player.getBoundingBox().expandTowards(player.getLookAngle().scale(range)).inflate(1.0),
				e -> e.isAlive() && e.isPickable() && !e.isSpectator() && e != player,
				range);
		if (entity == null || block.getType() != HitResult.Type.MISS) {
			return block;
		}
		return entity;
	}

	/**
	 * The living entity being looked at, or null when the sight line ends at
	 * a block or nothing. Never returns the caster themselves.
	 */
	public static LivingEntity findLivingTarget(ServerPlayer player, double range) {
		HitResult hit = raycast(player, range);
		if (hit.getType() != HitResult.Type.ENTITY || !(hit instanceof EntityHitResult entityHit)) {
			return null;
		}
		return entityHit.getEntity() instanceof LivingEntity living ? living : null;
	}
}
