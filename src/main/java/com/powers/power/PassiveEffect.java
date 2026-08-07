package com.powers.power;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;

/**
 * A status effect the player keeps while this power is assigned. The
 * server re-applies it on a refresh schedule so it never runs out
 */
public record PassiveEffect(Holder<MobEffect> effect, int amplifier) {
}
