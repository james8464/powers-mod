package com.powers.power.abilities;

import com.powers.PowersMod;
import com.powers.player.PlayerPowers;
import com.powers.progression.PowerScalingService;
import com.powers.power.Ability;
import com.powers.power.AmethystDampening;
import com.powers.power.PowerDamage;
import com.powers.power.PowerTargeting;
import com.powers.protection.PowerProtection;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Lightning Strike: smite whatever you're looking at with a bolt from
 * above, up to 64 blocks away.
 */
public class LightningStrikeAbility extends Ability {
	public LightningStrikeAbility() {
		super(PowersMod.id("lightning_strike"),
				Component.translatable("ability.powers.lightning_strike"),
				0, false);
	}

	@Override
	public boolean activate(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		var level = (net.minecraft.server.level.ServerLevel) player.level();
		double range = PowerScalingService.range(player, "lightning_strike", 64.0);
		HitResult hit = PowerTargeting.raycast(player, range);
		Vec3 target = hit.getLocation();
		if (hit.getType() == HitResult.Type.MISS) {
			// looking at the sky, so strike at full range instead
			target = player.getEyePosition().add(player.getLookAngle().scale(range));
		}

		BlockPos pos = BlockPos.containing(target);
		Vec3 strikePoint = Vec3.atCenterOf(pos);
		com.powers.fx.PowerFx.ring(level, strikePoint, 1.8, 0x4FC3F7, 20, 0);
		com.powers.fx.PowerFx.beam(level, strikePoint.add(0, 10, 0), strikePoint,
				net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK, 12);
		com.powers.fx.PowerFx.sound(level, strikePoint, net.minecraft.sounds.SoundEvents.BEACON_POWER_SELECT, 0.7f, 1.8f);
		LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
		if (bolt == null) {
			// couldn't spawn the bolt, so report failure and let the caller refund energy
			return false;
		}
		bolt.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
		bolt.setVisualOnly(true);
		level.addFreshEntity(bolt);
		Set<UUID> struck = new HashSet<>();
		for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class,
				AABB.ofSize(strikePoint, 5.0, 6.0, 5.0), e -> e.isAlive() && e != player
						&& !AmethystDampening.isDampened(e) && PowerProtection.mayHarm(player, e))) {
			struck.add(victim.getUUID());
			victim.hurtServer(level, PowerDamage.source(player),
					PowerScalingService.damage(player, "lightning_strike", 8.0f));
			if (victim.isInWater()) conduct(player, level, victim, struck);
		}
		return true;
	}

	/** Conducts through at most three wet-neighbour links, never revisiting a target. */
	private static void conduct(ServerPlayer player, net.minecraft.server.level.ServerLevel level,
			LivingEntity origin, Set<UUID> struck) {
		LivingEntity current = origin;
		for (int link = 0; link < 3; link++) {
			LivingEntity source = current;
			LivingEntity next = level.getEntitiesOfClass(LivingEntity.class,
					source.getBoundingBox().inflate(6.0), candidate -> candidate.isAlive() && candidate != player
							&& !struck.contains(candidate.getUUID()) && candidate.isInWater()
							&& !AmethystDampening.isDampened(candidate)
							&& PowerProtection.mayHarm(player, candidate)).stream()
					.min(java.util.Comparator.comparingDouble(source::distanceToSqr)).orElse(null);
			if (next == null) return;
			struck.add(next.getUUID());
			com.powers.fx.PowerFx.clash(level, current.getEyePosition(), next.getEyePosition(), 0x4FC3F7, 0xFFF59D);
			next.hurtServer(level, PowerDamage.source(player),
					PowerScalingService.damage(player, "lightning_strike", 4.0f));
			current = next;
		}
	}
}
