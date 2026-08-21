package com.powers;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** Registers the exhaustion and amethyst-poisoning status effects. */
public final class PowersEffects {
	public static final Holder<MobEffect> EXHAUSTION = Registry.registerForHolder(
			BuiltInRegistries.MOB_EFFECT,
			PowersMod.id("exhaustion"),
			new ExhaustionEffect());
	public static final Holder<MobEffect> AMETHYST_POISONING = Registry.registerForHolder(
			BuiltInRegistries.MOB_EFFECT,
			PowersMod.id("amethyst_poisoning"),
			new AmethystPoisoningEffect());

	private static final class ExhaustionEffect extends MobEffect {
		private ExhaustionEffect() {
			// A fixed deep-indigo tint distinguishes Exhaustion without ambient particles.
			super(MobEffectCategory.HARMFUL, 0x24104F);
		}
	}

	private static final class AmethystPoisoningEffect extends MobEffect {
		private AmethystPoisoningEffect() {
			// A fixed violet tint keeps Amethyst Poisoning visually distinct from Exhaustion.
			super(MobEffectCategory.HARMFUL, 0xB36BFF);
		}
	}

	private PowersEffects() {
	}

	public static void initialize() {
		// powers need this class loaded before they can apply the effects,
		// so initialize() just forces the class to initialize
	}
}
