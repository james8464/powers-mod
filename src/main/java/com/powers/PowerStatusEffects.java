package com.powers;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

/** Creates power-owned status effects without ambient entity particle clouds. */
public final class PowerStatusEffects {
	private PowerStatusEffects() {
	}

	/** Creates an effect with particles hidden and explicit ambient/icon presentation. */
	public static MobEffectInstance hidden(Holder<MobEffect> effect, int duration,
			int amplifier, boolean ambient, boolean showIcon) {
		return new MobEffectInstance(effect,
				com.powers.compat.ThirdPartyCombatCompatibility.effectDuration(duration),
				Math.clamp(amplifier, 0, 255), ambient, false, showIcon);
	}
}
