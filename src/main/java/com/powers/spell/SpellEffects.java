package com.powers.spell;

import com.powers.AmethystWardBlock;
import com.powers.PowersEffects;
import com.powers.fx.PowerFx;
import com.powers.player.LastDeathRecord;
import com.powers.player.PlayerPowers;
import com.powers.power.AmethystDampening;
import com.powers.power.PowerTargeting;
import com.powers.power.abilities.DimensionalAnchorAbility;
import com.powers.power.abilities.ForcefieldAbility;
import com.powers.power.crystals.SoulLinkAbility;
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

/** Server-authoritative effects for the active practical grimoire catalogue. */
final class SpellEffects {
	private SpellEffects() {
	}

	static SpellTarget acquireTarget(ServerPlayer caster, SpellDefinition spell) {
		double range = SpellCastValues.from(PowerScalingService.unranked(spell.id()))
				.targetRange();
		return switch (spell.effect()) {
			case CELESTIAL_RUIN -> SpellTarget.block(celestialTarget(caster, range));
			case DIMENSIONAL_ANCHOR, BLOOD_READING ->
					SpellTarget.entity(PowerTargeting.findLivingTarget(caster, range));
			case WARD_BREAKING_RITUAL -> SpellTarget.block(wardTarget(caster, range));
			case DISPEL -> {
				LivingEntity optional = PowerTargeting.findLivingTarget(caster, range);
				var field = SpellFieldManager.nearestDispelTarget(caster, range).orElse(null);
				if (field != null) caster.sendSystemMessage(Component.translatable(
						"spell.powers.dispel.inspect", field.displayName()), true);
				yield SpellTarget.dispel(optional, field);
			}
			default -> SpellTarget.none();
		};
	}

	static boolean execute(ServerPlayer caster, SpellDefinition spell,
			SpellTarget lockedTarget) {
		ServerLevel level = (ServerLevel) caster.level();
		SpellCastValues values = SpellCastValues.from(PowerScalingService.unranked(spell.id()));
		LivingEntity target = resolveEntity(caster, lockedTarget, values.targetRange());
		BlockPos focus = lockedTarget != null && lockedTarget.blockPos() != null
				? lockedTarget.blockPos() : target != null ? target.blockPosition() : caster.blockPosition();
		if (!PowerProtection.mayRitual(caster, level, focus)) return false;
		boolean success = switch (spell.effect()) {
			case AUGURY -> augury(caster);
			case CARTOGRAPHERS_STAR -> false;
			case HEARTH_SANCTUARY -> hearthSanctuary(caster);
			case BLOOD_READING -> bloodReading(caster, target);
			case GRAVE_RECALL -> graveRecall(caster);
			case VERDANT_TENDING -> verdantTending(caster, values);
			case CELESTIAL_RUIN -> celestialRuin(caster, lockedTarget.blockPos(), values.targetRange());
			case DIMENSIONAL_ANCHOR -> com.powers.entity.PlayerLikeTarget.isCompatible(target)
					&& PowerProtection.mayForceMove(caster, target)
					&& DimensionalAnchorAbility.apply(caster, target);
			case PURIFICATION_CIRCLE -> purification(caster, values.purificationRadius(), values.potencyTier());
			case WARD_BREAKING_RITUAL -> breakWard(caster, values, lockedTarget.blockPos());
			case DISPEL -> dispel(caster, target, values.targetRange(), lockedTarget.field());
			case SOUL_COMPASS -> false;
		};
		if (success) {
			Vec3 origin = caster.position().add(0, 1, 0);
			PowerFx.rune(level, origin, 1.9 * values.fieldRadius() / 7.0,
					color(spell.effect()), 22,
					level.getGameTime() * 0.04);
			PowerFx.spiral(level, origin, 0.65, 2.8,
					color(spell.effect()), 18, 0);
			PowerFx.sound(level, origin, SoundEvents.EVOKER_CAST_SPELL, 1.0f, 0.9f);
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
		if (death == null || !death.retained(caster.level().getGameTime())) {
			if (death != null) PlayerPowers.get(caster).clearLastDeath();
			caster.sendSystemMessage(Component.translatable("spell.powers.grave_recall.none"));
			return false;
		}
		caster.sendSystemMessage(Component.translatable("spell.powers.grave_recall.dimension",
				death.dimension()));
		caster.sendSystemMessage(Component.translatable("spell.powers.grave_recall.coordinates",
				death.x(), death.y(), death.z()));
		death.bearing(caster.level().dimension().identifier().toString(), caster.getX(), caster.getZ())
				.ifPresent(bearing -> caster.sendSystemMessage(
				Component.translatable("spell.powers.grave_recall.bearing", Component.translatable(bearing))));
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

	private static boolean dispel(ServerPlayer caster, LivingEntity target, double range,
			SpellFieldManager.DispelTarget inspectedField) {
		boolean field = SpellFieldManager.dispel(caster, range, inspectedField);
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
			case SOUL_COMPASS, AUGURY, CARTOGRAPHERS_STAR, CELESTIAL_RUIN -> 0xD9E9FF;
			case DIMENSIONAL_ANCHOR -> 0x665C99;
			case BLOOD_READING, GRAVE_RECALL -> 0x67405B;
			case PURIFICATION_CIRCLE, VERDANT_TENDING, HEARTH_SANCTUARY -> 0x65A765;
			case WARD_BREAKING_RITUAL, DISPEL -> 0x7455A8;
		};
	}
}
