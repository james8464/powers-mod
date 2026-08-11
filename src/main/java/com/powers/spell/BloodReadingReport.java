package com.powers.spell;

import com.powers.player.SkillSystem;
import com.powers.power.AmethystDampening;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

/** Bounded, chat-safe biological and magical facts for one living target. */
public record BloodReadingReport(float health, float maximumHealth, double armour,
		Alignment alignment, List<String> effectIds) {
	private static final int MAX_EFFECTS = 8;

	public enum Alignment { ORDINARY, DARKNESS, AMETHYST_DAMPENED }

	public BloodReadingReport {
		effectIds = List.copyOf(effectIds);
	}

	public static BloodReadingReport create(LivingEntity target) {
		List<String> effects = target.getActiveEffects().stream()
				.map(effect -> BuiltInRegistries.MOB_EFFECT.getKey(effect.getEffect().value()))
				.filter(java.util.Objects::nonNull).map(Object::toString).sorted().toList();
		return bounded(target.getHealth(), target.getMaxHealth(), target.getArmorValue(),
				target.entityTags().contains(SkillSystem.DARKNESS_TAG),
				AmethystDampening.isDampened(target), effects);
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
		return new BloodReadingReport(safeHealth, safeMaximum, safeArmour,
				alignment, boundedEffects);
	}

	public int healthPercent() {
		return maximumHealth <= 0.0F ? 0 : Math.clamp(Math.round(health * 100.0F / maximumHealth), 0, 100);
	}
}
