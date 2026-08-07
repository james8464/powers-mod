package com.powers.power;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Line-of-sight aiming shared by powers that need a target. Plain block
 * picking misses creatures, so this casts a real ray as well and takes
 * whichever hit is closer, keeping walls able to block your aim
 */
public final class PowerTargeting {
	private PowerTargeting() {
	}

	/**
	 * The nearest thing on the player's look line: a block, a creature,
	 * or a miss when nothing is in range
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
	 * The living creature the player is aiming at, or null when the line
	 * stops at a block or empty air. Never the caster themselves
	 */
	public static LivingEntity findLivingTarget(ServerPlayer player, double range) {
		HitResult hit = raycast(player, range);
		if (hit.getType() != HitResult.Type.ENTITY || !(hit instanceof EntityHitResult entityHit)) {
			return null;
		}
		return entityHit.getEntity() instanceof LivingEntity living ? living : null;
	}
}
