package com.powers.power.abilities;

import com.powers.PowersMod;
import com.powers.player.PlayerPowers;
import com.powers.power.Ability;
import com.powers.power.state.MagicShieldManager;
import com.powers.power.state.PowerEntityState;
import com.powers.progression.PowerScalingService;
import com.powers.progression.ScaledMagicValues;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import java.util.UUID;

/** Maintains timed personal wards that absorb damage and repel nearby threats. */
public class ForcefieldAbility extends Ability {
	// 8 seconds of shield
	private static final int DURATION = 160;
	private static final float BASE_INTEGRITY = 40.0f;

	public ForcefieldAbility() {
		super(PowersMod.id("forcefield"),
				Component.translatable("ability.powers.forcefield"),
				500, false);
	}

	@Override
	public boolean activate(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		ScaledMagicValues scaled = PowerScalingService.forPlayer(player, "forcefield");
		int duration = PowerScalingService.duration(player, "forcefield", DURATION);
		float integrity = (float) (BASE_INTEGRITY * scaled.potencyMultiplier());
		long expiry = player.level().getServer().getTickCount() + duration;
		MagicShieldManager.global().raise(player.getUUID(), integrity, expiry);
		// Fire resistance is thematic but finite integrity, not Resistance V,
		// owns normal hit prevention and can be broken by sustained pressure.
		player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, duration, 0, false, false));

		com.powers.fx.PowerFx.sound((net.minecraft.server.level.ServerLevel) player.level(),
				player.position(), net.minecraft.sounds.SoundEvents.BEACON_ACTIVATE, 0.8f, 0.5f);
		return true;
	}

	public static boolean absorbDamage(ServerPlayer player, DamageSource source, float amount) {
		long tick = player.level().getServer().getTickCount();
		MagicShieldManager.Impact impact = MagicShieldManager.global().absorb(player.getUUID(), amount, tick);
		if (!impact.blocked()) return false;
		ServerLevel level = (ServerLevel) player.level();
		Vec3 center = player.position().add(0, 1, 0);
		int color = impact.shattered() ? 0xE8F8FF : impact.fractureStage() == 0 ? 0x40C4FF : 0x8ADCF7;
		com.powers.fx.PowerFx.rune(level, center, impact.shattered() ? 2.0 : 1.4,
				color, impact.shattered() ? 30 : 18, tick * 0.1);
		com.powers.fx.PowerFx.burst(level, center, net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK,
				impact.shattered() ? 34 : 12, impact.shattered() ? 1.2 : 0.5, 0.08);
		com.powers.fx.PowerFx.sound(level, center, impact.shattered()
				? net.minecraft.sounds.SoundEvents.GLASS_BREAK : net.minecraft.sounds.SoundEvents.SHIELD_BLOCK.value(),
				1.0f, impact.shattered() ? 0.65f : 1.2f);
		if (PowerScalingService.forPlayer(player, "forcefield").unlockedVariants().contains("reflective_ward")) {
			Entity direct = source.getDirectEntity();
			if (direct instanceof Projectile projectile && PowerEntityState.tryReflect(projectile, 1)) {
				projectile.setOwner(player);
				projectile.setDeltaMovement(projectile.getDeltaMovement().scale(-1.15));
				projectile.hurtMarked = true;
			}
			Entity attacker = source.getEntity();
			if (attacker != null && attacker != player) {
				Vec3 away = attacker.position().subtract(player.position()).normalize().scale(0.65);
				attacker.push(away.x, 0.25, away.z);
				com.powers.fx.PowerFx.clash(level, center, attacker.position().add(0, 1, 0), 0x40C4FF, 0xFFFFFF);
			}
		}
		return true;
	}

	public static void tickAll(MinecraftServer server) {
		long tick = server.getTickCount();
		MagicShieldManager.global().expire(tick);
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (!player.isAlive() || !MagicShieldManager.global().active(player.getUUID(), tick)) continue;
			ServerLevel level = (ServerLevel) player.level();
			if (tick % 10 == 0) {
				int fracture = MagicShieldManager.global().fractureStage(player.getUUID(), tick);
				double phase = tick * 0.04;
				int color = fracture == 0 ? 0x40C4FF : fracture == 1 ? 0x8ADCF7 : 0xD6F5FF;
				com.powers.fx.PowerFx.ring(level, player.position().add(0, 0.15, 0), 1.5, 0x40C4FF, 20, phase);
				com.powers.fx.PowerFx.ring(level, player.position().add(0, 1.0, 0), 1.25, color, 20, -phase);
				com.powers.fx.PowerFx.ring(level, player.position().add(0, 1.85, 0), 1.5, color, 20, phase);
				if (fracture > 0) com.powers.fx.PowerFx.burst(level, player.position().add(0, 1, 0),
						net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK, 2 + fracture * 2, 0.5, 0.03);
			}
		}
	}

	public static void clear(UUID player) {
		MagicShieldManager.global().remove(player);
	}

	public static void clearAll() {
		MagicShieldManager.global().clear();
	}
}
