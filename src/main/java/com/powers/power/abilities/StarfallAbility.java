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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
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
		double range = PowerScalingService.range(player, "starfall", 64.0);
		HitResult hit = PowerTargeting.raycast(player, range);
		Vec3 target = hit.getLocation();
		if (hit.getType() == HitResult.Type.MISS) {
			// looking at the sky, so drop the shower at full range
			target = player.getEyePosition().add(player.getLookAngle().scale(range));
		}
		double stormRadius = scaledRange(player, 6.0);
		double strikeDiameter = scaledRange(player, 4.0);
		float strikeDamage = scaledPotency(player, 6.0f);
		com.powers.fx.PowerFx.ring(level, target, stormRadius, 0x3949AB, 30, 0);
		com.powers.fx.PowerFx.rune(level, target, stormRadius * 0.65, 0xFFF2B0, 28, Math.PI / 6);
		com.powers.fx.PowerFx.burst(level, target.add(0, 12, 0),
				net.minecraft.core.particles.ParticleTypes.END_ROD, 26, stormRadius * 0.3, 0.04);
		com.powers.fx.PowerFx.sound(level, target, SoundEvents.END_PORTAL_SPAWN, 0.8f, 1.55f);

		int boltCount = scaling(player).unlockedVariants().contains("empowered_impact") ? 4 : 3;
		for (int i = 0; i < boltCount; i++) {
			double dx = (level.getRandom().nextDouble() - 0.5) * stormRadius * 2.0;
			double dz = (level.getRandom().nextDouble() - 0.5) * stormRadius * 2.0;
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
					AABB.ofSize(strike, strikeDiameter, 5.0, strikeDiameter),
					e -> e.isAlive() && e != player
							&& !AmethystDampening.isDampened(e) && PowerProtection.mayHarm(player, e))) {
				victim.hurtServer(level, PowerDamage.source(player), strikeDamage);
			}
			com.powers.fx.PowerFx.spiral(level, strike, 0.7, 4.0, 0xFFF2B0, 18, i * Math.PI / 2.0);
		}
		return true;
	}
}
