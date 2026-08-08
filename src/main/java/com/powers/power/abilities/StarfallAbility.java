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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Starfall: a shower of celestial lightning rains down on the spot you're
 * looking at, striking in a wide ring around it.
 */
public class StarfallAbility extends Ability {
	public StarfallAbility() {
		super(PowersMod.id("starfall"),
				Component.translatable("ability.powers.starfall"),
				300, false);
	}

	@Override
	public boolean activate(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		ServerLevel level = (ServerLevel) player.level();
		double range = SkillSystem.range(player, 64.0);
		HitResult hit = PowerTargeting.raycast(player, range);
		Vec3 target = hit.getLocation();
		if (hit.getType() == HitResult.Type.MISS) {
			// looking at the sky, so drop the shower at full range
			target = player.getEyePosition().add(player.getLookAngle().scale(range));
		}
		com.powers.fx.PowerFx.ring(level, target, 4.0, 0x3949AB, 24, 0);
		com.powers.fx.PowerFx.burst(level, target.add(0, 12, 0),
				net.minecraft.core.particles.ParticleTypes.END_ROD, 18, 1.5, 0.04);

		// three bolts scattered up to 6 blocks from the target point
		for (int i = 0; i < 3; i++) {
			double dx = (level.getRandom().nextDouble() - 0.5) * 12.0;
			double dz = (level.getRandom().nextDouble() - 0.5) * 12.0;
			LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
			if (bolt == null) {
				continue;
			}
			BlockPos pos = BlockPos.containing(target.add(dx, 0, dz));
			bolt.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
			bolt.setVisualOnly(true);
			level.addFreshEntity(bolt);
			Vec3 strike = Vec3.atCenterOf(pos);
			for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class,
					AABB.ofSize(strike, 4.0, 5.0, 4.0), e -> e.isAlive() && e != player
							&& !AmethystDampening.isDampened(e) && PowerProtection.mayHarm(player, e))) {
				victim.hurtServer(level, PowerDamage.source(player), SkillSystem.damage(player, 6.0f));
			}
		}
		return true;
	}
}
