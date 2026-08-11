package com.powers.spell;

import com.powers.PowerStatusEffects;
import com.powers.AmethystWardBlock;
import com.powers.PowersEffects;
import com.powers.PowersMod;
import com.powers.fx.PowerFx;
import com.powers.player.LastDeathRecord;
import com.powers.player.PlayerPowers;
import com.powers.power.AmethystDampening;
import com.powers.power.PowerDamage;
import com.powers.power.PowerTargeting;
import com.powers.power.abilities.DimensionalAnchorAbility;
import com.powers.power.abilities.ForcefieldAbility;
import com.powers.power.crystals.SoulLinkAbility;
import com.powers.power.state.PowerEntityState;
import com.powers.protection.PowerProtection;
import com.powers.progression.PowerScalingService;
import com.powers.util.BoundedEntityCandidates;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Server-authoritative effects for the active practical grimoire catalogue. */
final class SpellEffects {
	private record Veil(long expiresAt) {
	}

	private static final Map<UUID, Veil> ACTIVE_VEILS = new HashMap<>();

	private SpellEffects() {
	}

	static SpellTarget acquireTarget(ServerPlayer caster, SpellDefinition spell) {
		double range = SpellCastValues.from(PowerScalingService.unranked(spell.id()), false)
				.targetRange();
		return switch (spell.effect()) {
			case CELESTIAL_RUIN -> SpellTarget.block(celestialTarget(caster, range));
			case TRACKING_MARK, DIMENSIONAL_ANCHOR, BINDING_SIGIL, VITALITY_TRANSFER, HEX,
					BLOOD_READING,
					ROOT_BINDING, BANISHMENT_CIRCLE, CONTROLLED_HELLFIRE ->
					SpellTarget.entity(PowerTargeting.findLivingTarget(caster, range));
			case WARD_BREAKING_RITUAL -> SpellTarget.block(wardTarget(caster, range));
			case DISPEL -> {
				LivingEntity optional = PowerTargeting.findLivingTarget(caster, range);
				yield optional == null ? SpellTarget.none() : SpellTarget.entity(optional);
			}
			default -> SpellTarget.none();
		};
	}

	static boolean execute(ServerPlayer caster, SpellDefinition spell, boolean amplified,
			SpellTarget lockedTarget) {
		ServerLevel level = (ServerLevel) caster.level();
		SpellCastValues values = SpellCastValues.from(
				PowerScalingService.unranked(spell.id()), amplified);
		LivingEntity target = resolveEntity(caster, lockedTarget, values.targetRange());
		boolean success = switch (spell.effect()) {
			case AUGURY -> augury(caster);
			case CARTOGRAPHERS_STAR -> false;
			case HEARTH_SANCTUARY -> hearthSanctuary(caster);
			case BLOOD_READING -> bloodReading(caster, target);
			case GRAVE_RECALL -> graveRecall(caster);
			case VERDANT_TENDING -> verdantTending(caster, values);
			case TRACKING_MARK -> trackingMark(caster, target, values.durationTicks());
			case WEATHER_SIGIL -> weatherSigil(caster, values);
			case CELESTIAL_RUIN -> celestialRuin(caster, lockedTarget.blockPos(), values.targetRange());
			case DIMENSIONAL_ANCHOR -> com.powers.entity.PlayerLikeTarget.isCompatible(target)
					&& PowerProtection.mayForceMove(caster, target)
					&& DimensionalAnchorAbility.apply(caster, target);
			case BINDING_SIGIL -> bind(caster, target, values, false);
			case ANTI_PORTAL_FIELD -> field(caster, SpellFieldKind.ANTI_PORTAL, values);
			case KINETIC_WARD -> field(caster, SpellFieldKind.KINETIC_WARD, values);
			case VITALITY_TRANSFER -> vitality(caster, target, values.damage());
			case HEX -> hex(caster, target, values);
			case CONCEALMENT_VEIL -> veil(caster, values.durationTicks());
			case PURIFICATION_CIRCLE -> purification(caster, values.purificationRadius(), values.potencyTier());
			case ROOT_BINDING -> bind(caster, target, values, true);
			case SANCTUARY_GROWTH -> field(caster, SpellFieldKind.SANCTUARY, values);
			case INFERNAL_SEAL -> field(caster, SpellFieldKind.INFERNAL_SEAL, values);
			case BANISHMENT_CIRCLE -> banish(caster, target, values);
			case CONTROLLED_HELLFIRE -> hellfire(caster, target, values);
			case WARD_BREAKING_RITUAL -> breakWard(caster, values, lockedTarget.blockPos());
			case DISPEL -> dispel(caster, target, values.targetRange());
			case RITUAL_AMPLIFICATION -> {
				SpellCastingManager.amplify(caster, values.durationTicks());
				yield true;
			}
			case COUNTERSPELL -> SpellCastingManager.counterspell(caster, values.targetRange());
			case SOUL_COMPASS -> false;
		};
		if (success) {
			Vec3 origin = caster.position().add(0, 1, 0);
			PowerFx.rune(level, origin, (amplified ? 2.6 : 1.9) * values.fieldRadius() / 7.0,
					color(spell.effect()), 22,
					level.getGameTime() * 0.04);
			PowerFx.spiral(level, origin, 0.65, amplified ? 4.0 : 2.8,
					color(spell.effect()), 18, 0);
			PowerFx.sound(level, origin, SoundEvents.EVOKER_CAST_SPELL, 1.0f, amplified ? 0.65f : 0.9f);
		}
		return success;
	}

	private static LivingEntity resolveEntity(ServerPlayer caster, SpellTarget target, double range) {
		if (target == null || target.entityId() == null) return null;
		var entity = ((ServerLevel) caster.level()).getEntityInAnyDimension(target.entityId());
		if (!(entity instanceof LivingEntity living)) return null;
		boolean valid = SpellTargetRules.remainsValid(living.isAlive(), living.level() == caster.level(),
				caster.hasLineOfSight(living), caster.distanceToSqr(living), range);
		return valid ? living : null;
	}

	private static boolean trackingMark(ServerPlayer caster, LivingEntity target, int duration) {
		if (target == null || AmethystDampening.isDampened(target)) return false;
		if (target instanceof ServerPlayer player && !PowerProtection.mayLocate(caster, player)) {
			com.powers.knowledge.MagicAttemptReporter.failure(caster, "tracking_mark",
					com.powers.knowledge.MagicFailureReason.CONSENT);
			return false;
		}
		target.addEffect(PowerStatusEffects.hidden(MobEffects.GLOWING, duration, 0, true, true));
		PowerFx.beam((ServerLevel) caster.level(), caster.getEyePosition(), target.position().add(0, 1, 0),
				ParticleTypes.END_ROD, 18);
		return true;
	}

	private static boolean weatherSigil(ServerPlayer caster, SpellCastValues values) {
		PowersMod.startStorm((ServerLevel) caster.level(), caster.position(),
				Math.max(40, values.durationTicks() / 10));
		return true;
	}

	private static boolean celestialRuin(ServerPlayer caster, BlockPos target, double range) {
		return blockTargetValid(caster, target, range) && CelestialRuinManager.begin(caster, target);
	}

	private static boolean augury(ServerPlayer caster) {
		ServerLevel level = (ServerLevel) caster.level();
		AuguryReport report = AuguryReport.create(level, caster.blockPosition());
		caster.sendSystemMessage(Component.translatable("spell.powers.augury.sky",
				report.weather().name().toLowerCase(java.util.Locale.ROOT), report.moon()));
		if (report.ticksUntilRealmEvent() >= 0L) {
			caster.sendSystemMessage(Component.translatable("spell.powers.augury.event",
					(report.ticksUntilRealmEvent() + 19L) / 20L));
		}
		caster.sendSystemMessage(Component.translatable("spell.powers.augury.force",
				report.darknessNear(), report.pureLightNear()));
		PowerFx.ring(level, caster.position().add(0.0, 0.08, 0.0), 3.2,
				0xD9E9FF, 28, level.getGameTime() * 0.04);
		PowerFx.sound(level, caster.position(), SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F, 1.55F);
		return true;
	}

	private static boolean bloodReading(ServerPlayer caster, LivingEntity target) {
		if (target == null || target == caster) return false;
		if (target instanceof ServerPlayer player && !PowerProtection.mayLocate(caster, player)) {
			com.powers.knowledge.MagicAttemptReporter.failure(caster, "blood_reading",
					com.powers.knowledge.MagicFailureReason.CONSENT);
			return false;
		}
		BloodReadingReport report = BloodReadingReport.create(target);
		caster.sendSystemMessage(Component.translatable("spell.powers.blood_reading.vitals",
				target.getDisplayName(), report.health(), report.maximumHealth(), report.healthPercent()));
		caster.sendSystemMessage(Component.translatable("spell.powers.blood_reading.warding",
				report.armour(), report.alignment().name().toLowerCase(java.util.Locale.ROOT)));
		caster.sendSystemMessage(Component.translatable("spell.powers.blood_reading.effects",
				report.effectIds().isEmpty() ? "none" : String.join(", ", report.effectIds())));
		ServerLevel level = (ServerLevel) caster.level();
		PowerFx.beam(level, caster.getEyePosition(), target.getEyePosition(),
				PowerFx.dust(0x9D1735, 0.8F), 12);
		PowerFx.rune(level, target.position().add(0.0, 0.1, 0.0), 1.3,
				0x9D1735, 18, level.getGameTime() * 0.05);
		return true;
	}

	private static boolean graveRecall(ServerPlayer caster) {
		LastDeathRecord death = PlayerPowers.get(caster).lastDeath();
		if (death == null) {
			caster.sendSystemMessage(Component.translatable("spell.powers.grave_recall.none"));
			return false;
		}
		caster.sendSystemMessage(Component.translatable("spell.powers.grave_recall.dimension",
				death.dimension()));
		caster.sendSystemMessage(Component.translatable("spell.powers.grave_recall.coordinates",
				death.x(), death.y(), death.z()));
		ServerLevel level = (ServerLevel) caster.level();
		PowerFx.rune(level, caster.position().add(0.0, 0.08, 0.0), 2.0,
				0x67405B, 24, level.getGameTime() * -0.05);
		PowerFx.sound(level, caster.position(), SoundEvents.SOUL_ESCAPE.value(), 0.8F, 0.65F);
		return true;
	}

	private static boolean verdantTending(ServerPlayer caster, SpellCastValues values) {
		ServerLevel level = (ServerLevel) caster.level();
		int radius = Math.clamp((int) Math.round(values.fieldRadius()), 2, 8);
		int inspected = 0;
		int changed = 0;
		for (BlockPos candidate : BlockPos.withinManhattan(caster.blockPosition(), radius, radius, radius)) {
			if (inspected++ >= VerdantTendingRules.MAX_INSPECTED_BLOCKS
					|| changed >= VerdantTendingRules.MAX_CHANGED_BLOCKS) break;
			BlockPos pos = candidate.immutable();
			BlockState state = level.getBlockState(pos);
			VerdantTendingRules.Action action = VerdantTendingRules.action(state);
			if (action == VerdantTendingRules.Action.NONE
					|| !PowerProtection.mayAffectBlock(caster, level, pos)) continue;
			boolean updated = switch (action) {
				case GROW -> grow(level, pos, state);
				case HYDRATE -> level.setBlock(pos,
						state.setValue(FarmlandBlock.MOISTURE, FarmlandBlock.MAX_MOISTURE), Block.UPDATE_CLIENTS);
				case EXTINGUISH -> level.removeBlock(pos, false);
				case NONE -> false;
			};
			if (!updated) continue;
			changed++;
			if (changed <= 12) PowerFx.burst(level, Vec3.atCenterOf(pos),
					PowerFx.dust(0x65A765, 0.75F), 3, 0.25, 0.0);
		}
		if (changed == 0) return false;
		PowerFx.ring(level, caster.position().add(0.0, 0.08, 0.0), radius,
				0x65A765, 30, level.getGameTime() * 0.03);
		PowerFx.sound(level, caster.position(), SoundEvents.BONE_MEAL_USE, 1.0F, 0.85F);
		caster.sendSystemMessage(Component.translatable("spell.powers.verdant_tending.changed", changed));
		return true;
	}

	private static boolean hearthSanctuary(ServerPlayer caster) {
		ServerLevel level = (ServerLevel) caster.level();
		ForcefieldAbility.raiseSpellWard(level, caster, HearthSanctuaryRules.INTEGRITY);
		int raised = 1;
		for (LivingEntity target : BoundedEntityCandidates.living(level,
				caster.getBoundingBox().inflate(HearthSanctuaryRules.RADIUS),
				HearthSanctuaryRules.MAX_TARGETS - 1,
				entity -> entity != caster && entity.isAlive() && !entity.isSpectator()
						&& HearthSanctuaryRules.withinRadius(entity.distanceToSqr(caster)))) {
			ForcefieldAbility.raiseSpellWard(level, target, HearthSanctuaryRules.INTEGRITY);
			raised++;
		}
		Vec3 center = caster.position().add(0.0, 0.08, 0.0);
		PowerFx.ring(level, center, HearthSanctuaryRules.RADIUS, 0xD8B85B,
				32, level.getGameTime() * 0.04);
		PowerFx.ring(level, center.add(0.0, 0.12, 0.0), 2.4, 0x5BE2D2,
				28, level.getGameTime() * -0.05);
		PowerFx.sound(level, center, SoundEvents.BEACON_ACTIVATE, 1.0F, 0.72F);
		caster.sendSystemMessage(Component.translatable("spell.powers.hearth_sanctuary.raised", raised));
		return true;
	}

	private static boolean grow(ServerLevel level, BlockPos pos, BlockState state) {
		if (!(state.getBlock() instanceof BonemealableBlock growable)
				|| !growable.isValidBonemealTarget(level, pos, state)) return false;
		growable.performBonemeal(level, level.getRandom(), pos, state);
		return !level.getBlockState(pos).equals(state);
	}

	private static BlockPos celestialTarget(ServerPlayer caster, double range) {
		HitResult hit = caster.pick(range, 0.0F, false);
		if (!(hit instanceof BlockHitResult blockHit)) {
			return null;
		}
		BlockPos target = blockHit.getBlockPos();
		return CelestialRuinManager.canBegin(caster.level(), target) ? target : null;
	}

	private static boolean bind(ServerPlayer caster, LivingEntity target, SpellCastValues values, boolean roots) {
		if (!offensiveAllowed(caster, target) || !PowerProtection.mayForceMove(caster, target)
				|| SpellFieldManager.blocksForcedMovement((ServerLevel) caster.level(), target,
						caster.getUUID())) return false;
		int tier = values.potencyTier();
		target.addEffect(PowerStatusEffects.hidden(MobEffects.SLOWNESS, values.durationTicks(),
				Math.min(6, (roots ? 4 : 2) + tier), true, true));
		target.addEffect(PowerStatusEffects.hidden(MobEffects.WEAKNESS, values.durationTicks(),
				Math.min(3, (roots ? 1 : 0) + tier / 2), true, true));
		PowerFx.ring((ServerLevel) caster.level(), target.position().add(0, 0.1, 0),
				1.2 + tier * 0.15, roots ? 0x477A3C : 0x513B78, 18, 0);
		return true;
	}

	private static boolean field(ServerPlayer caster, SpellFieldKind kind, SpellCastValues values) {
		SpellFieldManager.add(kind, caster, values.durationTicks(), values.fieldRadius(), values.potencyTier());
		return true;
	}

	private static boolean vitality(ServerPlayer caster, LivingEntity target, float damage) {
		if (!offensiveAllowed(caster, target)) return false;
		float healthBefore = target.getHealth();
		if (!target.hurtServer((ServerLevel) caster.level(), PowerDamage.source(caster), damage)) return false;
		caster.heal(Math.max(0.0f, healthBefore - target.getHealth()));
		PowerFx.beam((ServerLevel) caster.level(), target.position().add(0, 1, 0), caster.getEyePosition(),
				ParticleTypes.SOUL, 16);
		return true;
	}

	private static boolean hex(ServerPlayer caster, LivingEntity target, SpellCastValues values) {
		if (!offensiveAllowed(caster, target)) return false;
		int tier = values.potencyTier();
		target.addEffect(PowerStatusEffects.hidden(MobEffects.WEAKNESS, values.durationTicks(), 1 + tier, true, true));
		target.addEffect(PowerStatusEffects.hidden(MobEffects.SLOWNESS, values.durationTicks(), 1 + tier / 2, true, true));
		target.addEffect(PowerStatusEffects.hidden(MobEffects.DARKNESS,
				Math.min(values.durationTicks(), 240 + tier * 80), 0, true, true));
		PowerFx.spiral((ServerLevel) caster.level(), target.position(), 0.8, 2.2,
				0x67405B, 20, Math.PI / 4);
		return true;
	}

	private static boolean veil(ServerPlayer caster, int duration) {
		caster.addEffect(PowerStatusEffects.hidden(MobEffects.INVISIBILITY, duration, 0, true, true));
		ACTIVE_VEILS.put(caster.getUUID(), new Veil(caster.level().getGameTime() + duration));
		return true;
	}

	static boolean revealConcealment(ServerPlayer player) {
		Veil veil = ACTIVE_VEILS.remove(player.getUUID());
		if (veil == null) return false;
		MobEffectInstance current = player.getEffect(MobEffects.INVISIBILITY);
		long remaining = Math.max(0L, veil.expiresAt() - player.level().getGameTime());
		// Remove only the matching rank-zero veil. A longer/stronger potion that
		// replaced it belongs to another source and must remain untouched.
		if (current != null && current.getAmplifier() == 0 && current.getDuration() <= remaining + 5) {
			player.removeEffect(MobEffects.INVISIBILITY);
		}
		PowerFx.rune((ServerLevel) player.level(), player.position().add(0, 1, 0),
				1.1, 0x67405B, 18, Math.PI);
		return true;
	}

	static void expireVeils(long gameTime) {
		ACTIVE_VEILS.entrySet().removeIf(entry -> entry.getValue().expiresAt() <= gameTime);
	}

	static void clearVeil(UUID playerId) {
		ACTIVE_VEILS.remove(playerId);
	}

	static void clearAllVeils() {
		ACTIVE_VEILS.clear();
	}

	private static boolean purification(ServerPlayer caster, double radius, int potencyTier) {
		ServerLevel level = (ServerLevel) caster.level();
		for (LivingEntity ally : BoundedEntityCandidates.living(level,
				AABB.ofSize(caster.position(), radius * 2, radius * 2, radius * 2),
				256, LivingEntity::isAlive)) {
			if (!SpellTargetRules.mayPurify(ally == caster, caster.isAlliedTo(ally))) continue;
			for (MobEffectInstance instance : List.copyOf(ally.getActiveEffects())) {
				if (instance.getEffect().equals(PowersEffects.AMETHYST_POISONING)) continue;
				if (instance.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
					ally.removeEffect(instance.getEffect());
				}
			}
			ally.heal(4.0f + potencyTier * 2.0f);
			if (ally instanceof ServerPlayer player) {
				PlayerPowers.get(player).clearDimensionalAnchor();
				SoulLinkAbility.clearLinksTouching(player.getUUID());
			}
			PowerFx.burst(level, ally.position().add(0, 1, 0),
					PowerFx.dust(0xD8FFF1, 0.9F), 6, 0.4, 0.0);
		}
		return true;
	}

	private static boolean banish(ServerPlayer caster, LivingEntity target, SpellCastValues values) {
		if (!offensiveAllowed(caster, target) || !PowerProtection.mayForceMove(caster, target)
				|| SpellFieldManager.blocksForcedMovement((ServerLevel) caster.level(), target,
						caster.getUUID())) return false;
		if (PowerEntityState.isEphemeral(target)) {
			PowerFx.cancelled((ServerLevel) caster.level(), target.position().add(0, 1, 0), 0xC63C32);
			target.discard();
			return true;
		}
		Vec3 direction = target.position().subtract(caster.position()).normalize();
		target.setDeltaMovement(direction.x * values.banishForce(), 0.8,
				direction.z * values.banishForce());
		target.hurtServer((ServerLevel) caster.level(), PowerDamage.source(caster), values.damage() * 0.5f);
		return true;
	}

	private static boolean hellfire(ServerPlayer caster, LivingEntity target, SpellCastValues values) {
		if (!offensiveAllowed(caster, target)) return false;
		if (!target.hurtServer((ServerLevel) caster.level(), PowerDamage.source(caster), values.damage())) return false;
		target.igniteForSeconds(values.fireSeconds());
		PowerFx.burst((ServerLevel) caster.level(), target.position().add(0, 1, 0),
				ParticleTypes.SOUL_FIRE_FLAME, 24, 0.7, 0.08);
		return true;
	}

	private static boolean breakWard(ServerPlayer caster, SpellCastValues values, BlockPos ward) {
		if (!blockTargetValid(caster, ward, values.targetRange())
				|| !caster.level().getBlockState(ward).is(com.powers.PowersBlocks.AMETHYST_WARD)
				|| !AmethystWardBlock.isPowered(caster.level().getBlockState(ward))) return false;
		ServerLevel level = (ServerLevel) caster.level();
		AmethystDampening.suppressWard(level, ward, values.wardSuppressionTicks());
		PowerFx.cancelled(level, Vec3.atCenterOf(ward), 0xA66CFF);
		return true;
	}

	private static BlockPos wardTarget(ServerPlayer caster, double range) {
		HitResult hit = caster.pick(range, 0.0f, false);
		if (!(hit instanceof BlockHitResult blockHit)) return null;
		BlockPos pos = blockHit.getBlockPos();
		return caster.level().getBlockState(pos).is(com.powers.PowersBlocks.AMETHYST_WARD)
				&& AmethystWardBlock.isPowered(caster.level().getBlockState(pos)) ? pos : null;
	}

	private static boolean blockTargetValid(ServerPlayer caster, BlockPos target, double range) {
		if (target == null) return false;
		Vec3 start = caster.getEyePosition();
		Vec3 end = Vec3.atCenterOf(target);
		HitResult obstruction = caster.level().clip(new ClipContext(start, end,
				ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
		boolean visible = obstruction.getType() == HitResult.Type.MISS
				|| obstruction instanceof BlockHitResult block && block.getBlockPos().equals(target);
		return SpellTargetRules.remainsValid(true, true, visible, start.distanceToSqr(end), range);
	}

	private static boolean dispel(ServerPlayer caster, LivingEntity target, double range) {
		boolean field = SpellFieldManager.dispelNearest(caster, range);
		if (target == null) return field;
		if (target != caster && !caster.isAlliedTo(target) && !offensiveAllowed(caster, target)) return field;
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
			case SOUL_COMPASS, AUGURY, CARTOGRAPHERS_STAR, TRACKING_MARK, WEATHER_SIGIL,
					CELESTIAL_RUIN -> 0xD9E9FF;
			case DIMENSIONAL_ANCHOR, BINDING_SIGIL, ANTI_PORTAL_FIELD, KINETIC_WARD -> 0x665C99;
			case BLOOD_READING, GRAVE_RECALL, VITALITY_TRANSFER, HEX, CONCEALMENT_VEIL -> 0x67405B;
			case PURIFICATION_CIRCLE, VERDANT_TENDING, HEARTH_SANCTUARY, ROOT_BINDING,
					SANCTUARY_GROWTH -> 0x65A765;
			case INFERNAL_SEAL, BANISHMENT_CIRCLE, CONTROLLED_HELLFIRE -> 0xC63C32;
			case WARD_BREAKING_RITUAL, COUNTERSPELL, DISPEL, RITUAL_AMPLIFICATION -> 0x7455A8;
		};
	}
}
