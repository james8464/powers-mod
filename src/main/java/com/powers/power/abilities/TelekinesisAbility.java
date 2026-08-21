package com.powers.power.abilities;

import com.powers.PowersMod;
import com.powers.fx.PowerFx;
import com.powers.player.PlayerPowers;
import com.powers.power.Ability;
import com.powers.power.AmethystDampening;
import com.powers.power.state.PowerEntityState;
import com.powers.power.state.EntityFreezeController;
import com.powers.power.state.MagicShieldManager;
import com.powers.protection.PowerProtection;
import com.powers.spell.SpellFieldManager;
import com.powers.mind.BodyProxyManager;
import com.powers.util.PowerMessages;
import com.powers.util.BoundedEntityCandidates;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * telekinesis - psychically seize every living thing around you, yank it
 * into the air and fling it away, the way scarlet witch throws enemies
 * around
 */
public class TelekinesisAbility extends Ability {
	public TelekinesisAbility() {
		super(PowersMod.id("telekinesis"),
				Component.translatable("ability.powers.telekinesis"),
				240, false);
	}

	@Override
	public boolean activate(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		ServerLevel level = (ServerLevel) player.level();
		double range = scaledRange(player, 8.0);
		double force = Math.min(1.35, scaling(player).potencyMultiplier());
		AABB area = AABB.ofSize(player.position(), range * 2, range * 1.5, range * 2);
		// A centered finite volume bounds candidate inspection in every direction.
		Vec3 center = player.position();
		int moved = 0;
		for (LivingEntity target : BoundedEntityCandidates.living(level, area, 160,
				e -> mayMove(level, player, e))) {
			Vec3 fling = TelekinesisRules.outwardFling(
					center, target.position(), 2.2 * force, 0.7 * force);
			fling = ControlResistance.adjustImpulse(fling, ControlResistance.outcome(target));
			// Right on top of the player the radial direction is undefined, so the
			// target remains available for a later cast after either entity moves.
			if (fling.lengthSqr() == 0.0) continue;
			target.setDeltaMovement(target.getDeltaMovement().add(fling));
			// flag the velocity change so the client replays it and the throw looks instant
			target.hurtMarked = true;
			moved++;
			Vec3 targetCenter = target.position().add(0, target.getBbHeight() * 0.5, 0);
			PowerFx.beam(level, center.add(0, 1.2, 0), targetCenter,
					PowerFx.dust(0xC27CFF, 0.9F), 8);
			PowerFx.rune(level, targetCenter, 0.42, 0x9C27B0, 10, moved * 0.7);
			PowerFx.burst(level, targetCenter, PowerFx.dust(0xC27CFF, 0.85F), 5, 0.32, 0.0);
		}
		int intercepted = 0;
		for (Projectile projectile : BoundedEntityCandidates.collect(level,
				EntityTypeTest.forClass(Projectile.class), area, 64,
				x -> x.isAlive() && x.getOwner() != player)) {
			if (intercepted >= 16) break;
			if (!PowerEntityState.tryReflect(projectile, 1)) continue;
			intercepted++;
			projectile.setOwner(player);
			projectile.setDeltaMovement(player.getLookAngle().normalize().scale(1.8 * force));
			projectile.hurtMarked = true;
			PowerFx.beam(level, center.add(0, 1.2, 0), projectile.position(),
					ParticleTypes.ELECTRIC_SPARK, 6);
			PowerFx.rune(level, projectile.position(), 0.45, 0xC27CFF, 10, intercepted);
			PowerFx.burst(level, projectile.position(), PowerFx.dust(0x8FE9FF, 0.75F), 4, 0.2, 0.0);
		}
		if (!TelekinesisRules.resolved(moved, intercepted)) {
			PowerFx.cancelled(level, center.add(0, 1.0, 0), 0xC27CFF);
			PowerFx.rune(level, center.add(0, 0.08, 0), 0.55, 0x6E3A8A, 10, Math.PI);
			PowerMessages.send(player, "ability.powers.telekinesis.empty", 3);
			return false;
		}
		int impactParticles = Math.min(32, 14 + moved * 2 + intercepted);
		PowerFx.coloredBurst(level, center.add(0, 1.2, 0), 0x9C27B0, impactParticles, 0.8);
		PowerFx.burst(level, center.add(0, 1.0, 0), com.powers.PowersParticles.ECLIPSE, 10, 0.65, 0.05);
		PowerFx.rune(level, center, range * 0.45, 0xC27CFF, 24, player.tickCount * 0.08);
		PowerFx.ring(level, center.add(0, 0.12, 0), range * 0.30, 0x8FE9FF, 20,
				-player.tickCount * 0.11);
		float pitch = 0.84F + Math.min(0.22F, (moved + intercepted) * 0.015F);
		PowerFx.sound(level, center, net.minecraft.sounds.SoundEvents.EVOKER_CAST_SPELL, 1.0F, pitch);
		return true;
	}

	private static boolean mayMove(ServerLevel level, ServerPlayer caster, LivingEntity target) {
		return target.isAlive() && target != caster && !AmethystDampening.isDampened(target)
				&& !BodyProxyManager.isProxy(target) && !EntityFreezeController.isFrozen(target)
				&& PowerProtection.mayForceMove(caster, target)
				&& !SpellFieldManager.blocksForcedMovement(level, target, caster.getUUID())
				&& !MagicShieldManager.global().active(
						target.getUUID(), level.getServer().getTickCount());
	}
}
