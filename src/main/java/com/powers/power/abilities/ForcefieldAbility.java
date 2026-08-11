package com.powers.power.abilities;

import com.powers.PowersMod;
import com.powers.entity.PlayerLikeTarget;
import com.powers.player.PlayerPowers;
import com.powers.power.Ability;
import com.powers.power.state.MagicShieldManager;
import com.powers.power.state.PowerEntityState;
import com.powers.progression.ScaledMagicValues;
import com.powers.protection.PowerProtection;
import com.powers.spell.SpellFieldManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import java.util.UUID;

/** Shares integrity-owned wards that fully absorb even the impact that breaks them. */
public class ForcefieldAbility extends Ability {
	private static final float BASE_INTEGRITY = 40.0f;

	public ForcefieldAbility() {
		super(PowersMod.id("forcefield"),
				Component.translatable("ability.powers.forcefield"),
				500, false);
	}

	@Override
	public boolean activate(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		ScaledMagicValues scaled = scaling(player);
		float integrity = (float) (BASE_INTEGRITY * innateLevel(player).capacityMultiplier());
		boolean reflective = scaled.unlockedVariants().contains("reflective_ward");
		ServerLevel level = (ServerLevel) player.level();
		// The caster is never displaced by a crowded entity-section prefix.
		raiseWard(level, player, integrity, reflective);
		for (LivingEntity protectedTarget : com.powers.util.BoundedEntityCandidates.living(
				level, player.getBoundingBox().inflate(2.0), 16,
				target -> target != player && target.isAlive() && !target.isSpectator()
						&& PlayerLikeTarget.isCompatible(target)
						&& ForcefieldRules.withinSharingRadius(target.distanceToSqr(player)))) {
			raiseWard(level, protectedTarget, integrity, reflective);
		}
		com.powers.fx.PowerFx.sound(level,
				player.position(), net.minecraft.sounds.SoundEvents.BEACON_ACTIVATE, 0.8f, 0.5f);
		return true;
	}

	private static void raiseWard(ServerLevel level, LivingEntity target,
			float integrity, boolean reflective) {
		MagicShieldManager.global().raise(target.getUUID(), integrity,
				ForcefieldRules.expiryTick(), reflective);
		com.powers.fx.PowerFx.rune(level, target.position().add(0.0, 1.0, 0.0),
				1.45, 0x40C4FF, 22, level.getGameTime() * 0.1);
	}

	/** Raises a fixed-integrity, non-reflective ward for an unranked spell target. */
	public static void raiseSpellWard(ServerLevel level, LivingEntity target, float integrity) {
		raiseWard(level, target, integrity, false);
	}

	public static boolean absorbDamage(LivingEntity target, DamageSource source, float amount) {
		return absorbDamage(target, target, source, amount);
	}

	/** Lets a detached physical body consume the shield owned by its remote mind. */
	public static boolean absorbDamage(LivingEntity shieldOwner, LivingEntity visualTarget,
			DamageSource source, float amount) {
		long tick = visualTarget.level().getServer().getTickCount();
		MagicShieldManager.Impact impact = MagicShieldManager.global().absorb(
				shieldOwner.getUUID(), amount, tick);
		if (!impact.blocked()) return false;
		ServerLevel level = (ServerLevel) visualTarget.level();
		Vec3 center = visualTarget.position().add(0, 1, 0);
		int color = impact.shattered() ? 0xE8F8FF : impact.fractureStage() == 0 ? 0x40C4FF : 0x8ADCF7;
		com.powers.fx.PowerFx.rune(level, center, impact.shattered() ? 2.0 : 1.4,
				color, impact.shattered() ? 30 : 18, tick * 0.1);
		com.powers.fx.PowerFx.burst(level, center, net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK,
				impact.shattered() ? 34 : 12, impact.shattered() ? 1.2 : 0.5, 0.08);
		com.powers.fx.PowerFx.sound(level, center, impact.shattered()
				? net.minecraft.sounds.SoundEvents.GLASS_BREAK : net.minecraft.sounds.SoundEvents.SHIELD_BLOCK.value(),
				1.0f, impact.shattered() ? 0.65f : 1.2f);
		if (impact.reflective()) {
			Entity direct = source.getDirectEntity();
			if (direct instanceof Projectile projectile && PowerEntityState.tryReflect(projectile, 1)) {
				projectile.setOwner(shieldOwner);
				projectile.setDeltaMovement(projectile.getDeltaMovement().scale(-1.15));
				projectile.hurtMarked = true;
			}
			Entity attacker = source.getEntity();
			if (shieldOwner instanceof ServerPlayer player
					&& attacker instanceof net.minecraft.world.entity.LivingEntity living && attacker != player
					&& PowerProtection.mayForceMove(player, living)
					&& !SpellFieldManager.blocksForcedMovement(level, living, player.getUUID())) {
				Vec3 away = attacker.position().subtract(visualTarget.position()).normalize().scale(0.65);
				attacker.push(away.x, 0.25, away.z);
				com.powers.fx.PowerFx.clash(level, center, attacker.position().add(0, 1, 0), 0x40C4FF, 0xFFFFFF);
			}
		}
		return true;
	}

	public static void tickAll(MinecraftServer server) {
		long tick = server.getTickCount();
		if (tick % 10 != 0) {
			MagicShieldManager.global().expire(tick);
			return;
		}
		for (UUID owner : MagicShieldManager.global().activeOwners(tick)) {
			LivingEntity target = findLiving(server, owner);
			if (target == null || !target.isAlive()) continue;
			ServerLevel level = (ServerLevel) target.level();
			int fracture = MagicShieldManager.global().fractureStage(owner, tick);
			double phase = tick * 0.04;
			int color = fracture == 0 ? 0x40C4FF : fracture == 1 ? 0x8ADCF7 : 0xD6F5FF;
			com.powers.fx.PowerFx.ring(level, target.position().add(0, 0.15, 0), 1.5, 0x40C4FF, 20, phase);
			com.powers.fx.PowerFx.ring(level, target.position().add(0, 1.0, 0), 1.25, color, 20, -phase);
			com.powers.fx.PowerFx.ring(level, target.position().add(0, 1.85, 0), 1.5, color, 20, phase);
			if (fracture > 0) com.powers.fx.PowerFx.burst(level, target.position().add(0, 1, 0),
					net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK, 2 + fracture * 2, 0.5, 0.03);
		}
	}

	private static LivingEntity findLiving(MinecraftServer server, UUID owner) {
		for (ServerLevel level : server.getAllLevels()) {
			Entity entity = level.getEntity(owner);
			if (entity instanceof LivingEntity living) return living;
		}
		return null;
	}

	public static void clear(UUID player) {
		MagicShieldManager.global().remove(player);
	}

	public static void clearAll() {
		MagicShieldManager.global().clear();
	}
}
