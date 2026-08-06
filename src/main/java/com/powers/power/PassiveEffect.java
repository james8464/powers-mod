package com.powers.power;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;

/**
 * A permanent status effect granted while the owning power is assigned.
 * Applied server-side on a refresh schedule so it never expires.
 */
public record PassiveEffect(Holder<MobEffect> effect, int amplifier) {
}
