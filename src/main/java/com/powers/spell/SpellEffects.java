package com.powers.spell;

import com.powers.AmethystWardBlock;
import com.powers.PowersEffects;
import com.powers.PowersMod;
import com.powers.fx.PowerFx;
import com.powers.player.PlayerPowers;
import com.powers.power.AmethystDampening;
import com.powers.power.PowerDamage;
import com.powers.power.PowerTargeting;
import com.powers.power.abilities.DimensionalAnchorAbility;
import com.powers.protection.PowerProtection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Concrete, original spell suite shared by all six grimoires. */
final class SpellEffects {
	private static final double RANGE = 32.0;

	private SpellEffects() {
	}

	static boolean canBegin(ServerPlayer caster, SpellEffect effect) {
		return switch (effect) {
			case TRACKING_MARK, DIMENSIONAL_ANCHOR, BINDING_SIGIL, VITALITY_TRANSFER, HEX,
					ROOT_BINDING, BANISHMENT_CIRCLE, CONTROLLED_HELLFIRE ->
					PowerTargeting.findLivingTarget(caster, RANGE) != null;
			case WARD_BREAKING_RITUAL -> wardTarget(caster) != null;
			default -> true;
		};
	}

	static boolean execute(ServerPlayer caster, SpellDefinition spell, boolean amplified) {
		ServerLevel level = (ServerLevel) caster.level();
		LivingEntity target = PowerTargeting.findLivingTarget(caster, RANGE);
		int duration = amplified ? 900 : 600;
		float damage = amplified ? 9.0f : 6.0f;
		boolean success = switch (spell.effect()) {
			case TRACKING_MARK -> trackingMark(caster, target, duration);
			case WEATHER_SIGIL -> weatherSigil(caster, amplified);
			case DIMENSIONAL_ANCHOR -> target instanceof ServerPlayer player
					&& PowerProtection.mayForceMove(caster, player)
					&& DimensionalAnchorAbility.apply(caster, player);
			case BINDING_SIGIL -> bind(caster, target, duration, false);
			case ANTI_PORTAL_FIELD -> field(caster, SpellFieldKind.ANTI_PORTAL, duration);
			case KINETIC_WARD -> field(caster, SpellFieldKind.KINETIC_WARD, duration);
			case VITALITY_TRANSFER -> vitality(caster, target, damage);
			case HEX -> hex(caster, target, duration);
			case CONCEALMENT_VEIL -> veil(caster, duration);
			case PURIFICATION_CIRCLE -> purification(caster, amplified ? 12 : 8);
			case ROOT_BINDING -> bind(caster, target, duration, true);
			case SANCTUARY_GROWTH -> field(caster, SpellFieldKind.SANCTUARY, duration);
			case INFERNAL_SEAL -> field(caster, SpellFieldKind.INFERNAL_SEAL, duration);
			case BANISHMENT_CIRCLE -> banish(caster, target, amplified);
			case CONTROLLED_HELLFIRE -> hellfire(caster, target, damage);
			case WARD_BREAKING_RITUAL -> breakWard(caster, amplified);
			case DISPEL -> dispel(caster, target);
			case RITUAL_AMPLIFICATION -> {
				SpellCastingManager.amplify(caster, duration);
				yield true;
			}
			case COUNTERSPELL -> SpellCastingManager.counterspell(caster, amplified ? 32 : 20);
			case SOUL_COMPASS -> false;
		};
		if (success) {
			Vec3 origin = caster.position().add(0, 1, 0);
			PowerFx.rune(level, origin, amplified ? 2.6 : 1.9, color(spell.effect()), 22,
					level.getGameTime() * 0.04);
			PowerFx.spiral(level, origin, 0.65, amplified ? 4.0 : 2.8,
					color(spell.effect()), 18, 0);
			PowerFx.sound(level, origin, SoundEvents.EVOKER_CAST_SPELL, 1.0f, amplified ? 0.65f : 0.9f);
		}
		return success;
	}

	private static boolean trackingMark(ServerPlayer caster, LivingEntity target, int duration) {
		if (target == null || AmethystDampening.isDampened(target)) return false;
		if (target instanceof ServerPlayer player && !PowerProtection.mayLocate(caster, player)) return false;
		target.addEffect(new MobEffectInstance(MobEffects.GLOWING, duration, 0, true, false));
		PowerFx.beam((ServerLevel) caster.level(), caster.getEyePosition(), target.position().add(0, 1, 0),
				ParticleTypes.END_ROD, 18);
		return true;
	}

	private static boolean weatherSigil(ServerPlayer caster, boolean amplified) {
		PowersMod.startStorm((ServerLevel) caster.level(), caster.position(), amplified ? 100 : 60);
		return true;
	}

	private static boolean bind(ServerPlayer caster, LivingEntity target, int duration, boolean roots) {
		if (!offensiveAllowed(caster, target) || !PowerProtection.mayForceMove(caster, target)) return false;
		target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, duration, roots ? 5 : 3, true, false));
		target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration, roots ? 1 : 0, true, false));
		PowerFx.ring((ServerLevel) caster.level(), target.position().add(0, 0.1, 0),
				1.2, roots ? 0x477A3C : 0x513B78, 18, 0);
		return true;
	}

	private static boolean field(ServerPlayer caster, SpellFieldKind kind, int duration) {
		SpellFieldManager.add(kind, caster, duration);
		return true;
	}

	private static boolean vitality(ServerPlayer caster, LivingEntity target, float damage) {
		if (!offensiveAllowed(caster, target)) return false;
		if (!target.hurtServer((ServerLevel) caster.level(), PowerDamage.source(caster), damage)) return false;
		caster.heal(damage);
		PowerFx.beam((ServerLevel) caster.level(), target.position().add(0, 1, 0), caster.getEyePosition(),
				ParticleTypes.SOUL, 16);
		return true;
	}

	private static boolean hex(ServerPlayer caster, LivingEntity target, int duration) {
		if (!offensiveAllowed(caster, target)) return false;
		target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration, 1, true, false));
		target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, duration, 1, true, false));
		target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, Math.min(duration, 240), 0, true, false));
		return true;
	}

	private static boolean veil(ServerPlayer caster, int duration) {
		caster.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, duration, 0, true, false));
		return true;
	}

	private static boolean purification(ServerPlayer caster, int radius) {
		ServerLevel level = (ServerLevel) caster.level();
		for (LivingEntity ally : level.getEntitiesOfClass(LivingEntity.class,
				AABB.ofSize(caster.position(), radius * 2, radius * 2, radius * 2), LivingEntity::isAlive)) {
			if (!caster.isAlliedTo(ally) && ally != caster && ally instanceof ServerPlayer) continue;
			for (MobEffectInstance instance : List.copyOf(ally.getActiveEffects())) {
				if (instance.getEffect().equals(PowersEffects.AMETHYST_POISONING)) continue;
				if (instance.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
					ally.removeEffect(instance.getEffect());
				}
			}
			ally.heal(4.0f);
			PowerFx.burst(level, ally.position().add(0, 1, 0), ParticleTypes.HAPPY_VILLAGER, 6, 0.4, 0.03);
		}
		return true;
	}

	private static boolean banish(ServerPlayer caster, LivingEntity target, boolean amplified) {
		if (!offensiveAllowed(caster, target) || !PowerProtection.mayForceMove(caster, target)) return false;
		Vec3 direction = target.position().subtract(caster.position()).normalize();
		target.setDeltaMovement(direction.x * (amplified ? 4.0 : 2.5), 0.8, direction.z * (amplified ? 4.0 : 2.5));
		target.hurtServer((ServerLevel) caster.level(), PowerDamage.source(caster), amplified ? 6.0f : 3.0f);
		return true;
	}

	private static boolean hellfire(ServerPlayer caster, LivingEntity target, float damage) {
		if (!offensiveAllowed(caster, target)) return false;
		if (!target.hurtServer((ServerLevel) caster.level(), PowerDamage.source(caster), damage)) return false;
		target.igniteForSeconds(6);
		PowerFx.burst((ServerLevel) caster.level(), target.position().add(0, 1, 0),
				ParticleTypes.SOUL_FIRE_FLAME, 24, 0.7, 0.08);
		return true;
	}

	private static boolean breakWard(ServerPlayer caster, boolean amplified) {
		BlockPos ward = wardTarget(caster);
		if (ward == null) return false;
		ServerLevel level = (ServerLevel) caster.level();
		AmethystDampening.suppressWard(level, ward, amplified ? 1800 : 900);
		PowerFx.cancelled(level, Vec3.atCenterOf(ward), 0xA66CFF);
		return true;
	}

	private static BlockPos wardTarget(ServerPlayer caster) {
		HitResult hit = caster.pick(RANGE, 0.0f, false);
		if (!(hit instanceof BlockHitResult blockHit)) return null;
		BlockPos pos = blockHit.getBlockPos();
		return caster.level().getBlockState(pos).is(com.powers.PowersBlocks.AMETHYST_WARD)
				&& AmethystWardBlock.isPowered(caster.level().getBlockState(pos)) ? pos : null;
	}

	private static boolean dispel(ServerPlayer caster, LivingEntity target) {
		boolean field = SpellFieldManager.dispelNearest(caster, RANGE);
		if (target == null) return field;
		for (MobEffectInstance instance : List.copyOf(target.getActiveEffects())) {
			if (!instance.getEffect().equals(PowersEffects.AMETHYST_POISONING)) {
				target.removeEffect(instance.getEffect());
			}
		}
		if (target instanceof ServerPlayer player) PlayerPowers.get(player).clearDimensionalAnchor();
		return true;
	}

	private static boolean offensiveAllowed(ServerPlayer caster, LivingEntity target) {
		return target != null && target.isAlive() && !AmethystDampening.isDampened(target)
				&& PowerProtection.mayHarm(caster, target)
				&& !SpellFieldManager.isSanctuaryProtected((ServerLevel) target.level(), target);
	}

	private static int color(SpellEffect effect) {
		return switch (effect) {
			case SOUL_COMPASS, TRACKING_MARK, WEATHER_SIGIL -> 0xD9E9FF;
			case DIMENSIONAL_ANCHOR, BINDING_SIGIL, ANTI_PORTAL_FIELD, KINETIC_WARD -> 0x665C99;
			case VITALITY_TRANSFER, HEX, CONCEALMENT_VEIL -> 0x67405B;
			case PURIFICATION_CIRCLE, ROOT_BINDING, SANCTUARY_GROWTH -> 0x65A765;
			case INFERNAL_SEAL, BANISHMENT_CIRCLE, CONTROLLED_HELLFIRE -> 0xC63C32;
			case WARD_BREAKING_RITUAL, COUNTERSPELL, DISPEL, RITUAL_AMPLIFICATION -> 0x7455A8;
		};
	}
}
