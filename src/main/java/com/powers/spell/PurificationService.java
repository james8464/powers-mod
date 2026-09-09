package com.powers.spell;

import com.powers.PowersEffects;
import com.powers.fx.PowerFx;
import com.powers.player.PlayerPowers;
import com.powers.power.crystals.SoulLinkAbility;
import com.powers.util.BoundedEntityCandidates;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.List;

/** Applies exactly one bounded, ally-only purification mode without consuming a catalyst. */
public final class PurificationService {
	private PurificationService() {
	}

	public static boolean apply(ServerPlayer caster, double radius, int potencyTier,
			PurificationModeRules.Mode mode) {
		if (caster == null || !(caster.level() instanceof ServerLevel level)
				|| !Double.isFinite(radius) || radius <= 0.0) return false;
		PurificationModeRules.Mode selected = mode == null
				? PurificationModeRules.Mode.CLEANSE : mode;
		applyOne(level, caster, selected, potencyTier);
		for (LivingEntity ally : BoundedEntityCandidates.living(level,
				AABB.ofSize(caster.position(), radius * 2, radius * 2, radius * 2),
				255, LivingEntity::isAlive)) {
			if (ally == caster) continue;
			if (!SpellTargetRules.mayPurify(ally == caster, caster.isAlliedTo(ally))) continue;
			applyOne(level, ally, selected, potencyTier);
		}
		return true;
	}

	private static void applyOne(ServerLevel level, LivingEntity ally,
			PurificationModeRules.Mode selected, int potencyTier) {
		ally.heal(4.0F + Math.max(0, potencyTier) * 2.0F);
		switch (selected) {
			case CLEANSE -> cleanse(ally);
			case LINK_SEVER -> SoulLinkAbility.clearLinksTouching(ally.getUUID());
			case CORRUPTION_RELIEF -> relieveCorruption(ally);
		}
		PowerFx.burst(level, ally.position().add(0, 1, 0),
				PowerFx.dust(0xD8FFF1, 0.9F), 6, 0.4, 0.0);
	}

	private static void cleanse(LivingEntity ally) {
		for (var instance : List.copyOf(ally.getActiveEffects())) {
			boolean amethyst = instance.getEffect().equals(PowersEffects.AMETHYST_POISONING);
			boolean corruption = instance.getEffect().equals(PowersEffects.EXHAUSTION);
			boolean harmful = instance.getEffect().value().getCategory() == MobEffectCategory.HARMFUL;
			if (PurificationModeRules.removesEffect(PurificationModeRules.Mode.CLEANSE,
					harmful, amethyst, corruption)) ally.removeEffect(instance.getEffect());
		}
	}

	private static void relieveCorruption(LivingEntity ally) {
		if (PurificationModeRules.removesEffect(PurificationModeRules.Mode.CORRUPTION_RELIEF,
				true, false, ally.hasEffect(PowersEffects.EXHAUSTION))) {
			ally.removeEffect(PowersEffects.EXHAUSTION);
		}
		if (ally instanceof ServerPlayer player) PlayerPowers.get(player).clearDimensionalAnchor();
	}
}
