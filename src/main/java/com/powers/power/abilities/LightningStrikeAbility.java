package com.powers.power.abilities;

import com.powers.PowersMod;
import com.powers.player.PlayerPowers;
import com.powers.player.SkillSystem;
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
		double range = SkillSystem.range(player, 64.0);
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
		for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class,
				AABB.ofSize(strikePoint, 5.0, 6.0, 5.0), e -> e.isAlive() && e != player
						&& !AmethystDampening.isDampened(e) && PowerProtection.mayHarm(player, e))) {
			victim.hurtServer(level, PowerDamage.source(player), SkillSystem.damage(player, 8.0f));
		}
		return true;
	}
}
