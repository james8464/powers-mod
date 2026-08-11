package com.powers.magic.runtime;

import com.powers.PowerStatusEffects;
import com.powers.PowersEffects;
import com.powers.fx.PowerFx;
import com.powers.player.PlayerPowers;
import com.powers.power.AmethystDampening;
import com.powers.power.abilities.InvisibilityToggleAbility;
import com.powers.power.crystals.SoulLinkAbility;
import com.powers.power.state.MagicShieldManager;
import com.powers.power.state.PowerEntityState;
import com.powers.protection.PowerProtection;
import com.powers.spell.SpellCastingManager;
import com.powers.util.BoundedEntityCandidates;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Applies bounded, server-authoritative mechanics for named collision motifs. */
final class MagicReactionEffects {
	private static final int MAX_AFFECTED_ENTITIES = 32;

	private MagicReactionEffects() {
	}

	static void apply(ServerLevel level, MagicReactionEvent event, Vec3 midpoint) {
		String motif = event.resolution().cue().motif();
		double radius = Math.min(8.0, 2.0 + event.resolution().cue().intensity() * 0.8);
		switch (motif) {
			case "steam" -> steam(level, midpoint, radius);
			case "eclipse", "revealed_veil" -> reveal(level, midpoint, radius);
			case "star_rift" -> consumeProjectiles(level, midpoint, radius);
			case "return_seal" -> banishSummons(level, midpoint, radius * 2.0);
			case "purifying_severance" -> severSoulLinks(level, event, midpoint, radius);
			case "cleansing_rain" -> cleanse(level, midpoint, radius);
			case "ward_clash" -> fractureExistingWard(level, event);
			case "grounded_storm" -> groundStorm(level, midpoint);
			case "violent_interference" -> pressureWave(level, midpoint, radius);
			case "concordant_bloom" -> concordantBloom(level, midpoint, radius);
			default -> {
				// Multipliers are the mechanics for the remaining exhaustive cases;
				// their deterministic rune/clash presentation is emitted by the adapter.
			}
		}
	}

	private static void steam(ServerLevel level, Vec3 midpoint, double radius) {
		for (LivingEntity entity : living(level, midpoint, radius)) {
			if (!mayContest(level, entity)) continue;
			entity.clearFire();
			entity.addEffect(PowerStatusEffects.hidden(MobEffects.BLINDNESS, 30, 0, true, true));
		}
		PowerFx.burst(level, midpoint, ParticleTypes.CLOUD, 42, radius * 0.45, 0.025);
	}

	private static void reveal(ServerLevel level, Vec3 midpoint, double radius) {
		for (LivingEntity entity : living(level, midpoint, radius)) {
			if (!mayContest(level, entity)) continue;
			entity.addEffect(PowerStatusEffects.hidden(MobEffects.GLOWING, 60, 0, true, true));
			if (entity instanceof ServerPlayer player) {
				InvisibilityToggleAbility.reveal(player);
			}
		}
		PowerFx.ring(level, midpoint, radius, 0xFFF2B0, 28, Math.PI / 2);
	}

	private static void consumeProjectiles(ServerLevel level, Vec3 midpoint, double radius) {
		int removed = 0;
		for (Projectile projectile : BoundedEntityCandidates.ofClass(level, Projectile.class,
				bounds(midpoint, radius), MAX_AFFECTED_ENTITIES * 4, Projectile::isAlive)) {
			if (!PowerEntityState.isPowerProjectile(projectile)) continue;
			PowerFx.burst(level, projectile.position(), ParticleTypes.REVERSE_PORTAL, 8, 0.2, 0.02);
			projectile.discard();
			if (++removed >= MAX_AFFECTED_ENTITIES) break;
		}
		PowerFx.spiral(level, midpoint.add(0, -0.6, 0), radius * 0.45, 1.2,
				0xFFF2B0, 30, Math.PI / 4);
	}

	private static void banishSummons(ServerLevel level, Vec3 midpoint, double radius) {
		int removed = 0;
		for (Entity entity : BoundedEntityCandidates.ofClass(level, Entity.class,
				bounds(midpoint, radius), MAX_AFFECTED_ENTITIES * 4, Entity::isAlive)) {
			if (!PowerEntityState.isBanishableSummon(entity)) continue;
			PowerFx.burst(level, entity.position().add(0, 0.8, 0), ParticleTypes.POOF, 16, 0.6, 0.12);
			entity.discard();
			if (++removed >= MAX_AFFECTED_ENTITIES) break;
		}
	}

	private static void severSoulLinks(ServerLevel level, MagicReactionEvent event,
			Vec3 midpoint, double radius) {
		SoulLinkAbility.clear(event.cast().owner());
		SoulLinkAbility.clear(event.existing().owner());
		for (LivingEntity entity : living(level, midpoint, radius)) {
			SoulLinkAbility.clearLinksTouching(entity.getUUID());
		}
		PowerFx.burst(level, midpoint, ParticleTypes.SOUL, 30, radius * 0.35, 0.08);
	}

	private static void cleanse(ServerLevel level, Vec3 midpoint, double radius) {
		for (LivingEntity entity : living(level, midpoint, radius)) {
			entity.clearFire();
			for (MobEffectInstance effect : List.copyOf(entity.getActiveEffects())) {
				if (effect.getEffect().equals(PowersEffects.AMETHYST_POISONING)) continue;
				if (effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
					entity.removeEffect(effect.getEffect());
				}
			}
			if (entity instanceof ServerPlayer player) PlayerPowers.get(player).clearDimensionalAnchor();
			SoulLinkAbility.clearLinksTouching(entity.getUUID());
			PowerFx.burst(level, entity.position().add(0, 1, 0),
					PowerFx.dust(0xD8FFF1, 0.85F), 5, 0.35, 0.0);
		}
	}

	private static void fractureExistingWard(ServerLevel level, MagicReactionEvent event) {
		if (!event.existing().action().value().equals("forcefield")) return;
		MagicShieldManager.global().absorb(event.existing().owner(),
				4.0f + event.resolution().cue().intensity() * 2.0f, level.getServer().getTickCount());
	}

	private static void groundStorm(ServerLevel level, Vec3 midpoint) {
		PowerFx.beam(level, midpoint.add(0, 3.0, 0), midpoint,
				ParticleTypes.ELECTRIC_SPARK, 14);
		PowerFx.ring(level, midpoint, 2.4, 0xFFF59D, 22, 0.0);
	}

	private static void pressureWave(ServerLevel level, Vec3 midpoint, double radius) {
		for (LivingEntity entity : living(level, midpoint, radius)) {
			if (!mayContest(level, entity)) continue;
			Vec3 direction = entity.position().subtract(midpoint);
			if (direction.lengthSqr() < 0.01) direction = new Vec3(0, 1, 0);
			direction = direction.normalize().scale(0.45);
			entity.push(direction.x, Math.max(0.12, direction.y), direction.z);
		}
		PowerFx.burst(level, midpoint, ParticleTypes.GUST, 22, radius * 0.35, 0.1);
	}

	private static void concordantBloom(ServerLevel level, Vec3 midpoint, double radius) {
		for (LivingEntity entity : living(level, midpoint, radius)) {
			if (!entity.isAlive()) continue;
			entity.heal(1.0f);
		}
		PowerFx.burst(level, midpoint, PowerFx.dust(0xB9FFB1, 1.0F),
				18, radius * 0.35, 0.0);
	}

	private static List<LivingEntity> living(ServerLevel level, Vec3 midpoint, double radius) {
		List<LivingEntity> entities = BoundedEntityCandidates.living(level,
				bounds(midpoint, radius), MAX_AFFECTED_ENTITIES, LivingEntity::isAlive);
		return entities;
	}

	private static AABB bounds(Vec3 midpoint, double radius) {
		return AABB.ofSize(midpoint, radius * 2.0, radius * 2.0, radius * 2.0);
	}

	private static boolean mayContest(ServerLevel level, LivingEntity entity) {
		return !AmethystDampening.isDampened(entity) && !PowerProtection.isSafeZone(level, entity.position());
	}
}
