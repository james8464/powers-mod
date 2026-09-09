package com.powers.spell;

import com.powers.player.SkillSystem;
import com.powers.power.AmethystDampening;
import com.powers.power.state.MagicShieldManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

/** Bounded, chat-safe biological and magical facts for one living target. */
public record BloodReadingReport(float health, float maximumHealth, double armour,
		Alignment alignment, List<String> effectIds, float recentDamage, float recentHealing,
		BloodReadingTrendRules.Trend trend, ForceState forceState, AmethystState amethystState) {
	private static final int MAX_EFFECTS = 8;

	public enum Alignment { ORDINARY, DARKNESS, AMETHYST_DAMPENED }
	public enum ForceState { NONE, INTACT, CRACKED, FRACTURED }
	public enum AmethystState { EXPOSED, DAMPENED }

	public BloodReadingReport {
		effectIds = List.copyOf(effectIds);
	}

	public static BloodReadingReport create(LivingEntity target) {
		List<String> effects = target.getActiveEffects().stream()
				.map(effect -> BuiltInRegistries.MOB_EFFECT.getKey(effect.getEffect().value()))
				.filter(java.util.Objects::nonNull).map(Object::toString).sorted().toList();
		BloodReadingTrendRules.Summary history = BloodReadingHistoryService.summary(target);
		long tick = target.level() instanceof ServerLevel level ? level.getServer().getTickCount() : 0L;
		ForceState force = MagicShieldManager.global().snapshot(target.getUUID(), tick)
				.map(snapshot -> switch (snapshot.fractureStage()) {
					case 0 -> ForceState.INTACT;
					case 1 -> ForceState.CRACKED;
					default -> ForceState.FRACTURED;
				}).orElse(ForceState.NONE);
		return bounded(target.getHealth(), target.getMaxHealth(), target.getArmorValue(),
				target.entityTags().contains(SkillSystem.DARKNESS_TAG),
				AmethystDampening.isDampened(target), effects, history.recentDamage(),
				history.recentHealing(), history.trend(), force);
	}

	public static BloodReadingReport bounded(float health, float maximumHealth, double armour,
			boolean darkness, boolean dampened, List<String> effects) {
		float safeMaximum = Float.isFinite(maximumHealth) ? Math.max(0.0F, maximumHealth) : 0.0F;
		float safeHealth = Float.isFinite(health) ? Math.clamp(health, 0.0F, safeMaximum) : 0.0F;
		double safeArmour = Double.isFinite(armour) ? Math.max(0.0, armour) : 0.0;
		Alignment alignment = dampened ? Alignment.AMETHYST_DAMPENED
				: darkness ? Alignment.DARKNESS : Alignment.ORDINARY;
		List<String> boundedEffects = effects == null ? List.of()
				: effects.stream().filter(java.util.Objects::nonNull).limit(MAX_EFFECTS).toList();
		return new BloodReadingReport(safeHealth, safeMaximum, safeArmour, alignment,
				boundedEffects, 0.0F, 0.0F, BloodReadingTrendRules.Trend.STABLE,
				ForceState.NONE, dampened ? AmethystState.DAMPENED : AmethystState.EXPOSED);
	}

	public static BloodReadingReport bounded(float health, float maximumHealth, double armour,
			boolean darkness, boolean dampened, List<String> effects, float recentDamage,
			float recentHealing, BloodReadingTrendRules.Trend trend, ForceState forceState) {
		BloodReadingReport base = bounded(health, maximumHealth, armour, darkness, dampened, effects);
		float safeDamage = Float.isFinite(recentDamage) ? Math.max(0.0F, recentDamage) : 0.0F;
		float safeHealing = Float.isFinite(recentHealing) ? Math.max(0.0F, recentHealing) : 0.0F;
		return new BloodReadingReport(base.health(), base.maximumHealth(), base.armour(), base.alignment(),
				base.effectIds(), safeDamage, safeHealing,
				trend == null ? BloodReadingTrendRules.Trend.STABLE : trend,
				forceState == null ? ForceState.NONE : forceState, base.amethystState());
	}

	public int healthPercent() {
		return maximumHealth <= 0.0F ? 0 : Math.clamp(Math.round(health * 100.0F / maximumHealth), 0, 100);
	}
}
