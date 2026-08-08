package com.powers.power;

import com.powers.PowersMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;

/**
 * The damage every offensive ability deals, and the test that recognises it
 * again afterwards.
 *
 * <p>Amethyst is the enemy of <em>powers</em>, not of swords. Blocking every
 * source that happens to come from a player would hand anyone carrying a shard
 * total PvP immunity, so dampening keys off this one predicate instead: ability
 * damage is tagged as indirect magic with the caster attached, and only that is
 * turned aside.
 */
public final class PowerDamage {
	public static final ResourceKey<DamageType> POWER_MAGIC = ResourceKey.create(
			Registries.DAMAGE_TYPE, PowersMod.id("power_magic"));
	public static final TagKey<DamageType> POWER_DAMAGE = TagKey.create(
			Registries.DAMAGE_TYPE, PowersMod.id("power_damage"));

	private PowerDamage() {
	}

	/**
	 * The damage source for anything a player's power inflicts. The caster is
	 * attached as both the direct and the causing entity so kill attribution,
	 * advancements and death messages still credit them.
	 */
	public static DamageSource source(ServerPlayer caster) {
		var type = caster.level().registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE)
				.getOrThrow(POWER_MAGIC);
		return new DamageSource(type, caster, caster);
	}

	/**
	 * Whether this damage came out of a power rather than an ordinary attack.
	 * Covers the freeze typing Ice Manipulation used to carry and the plain
	 * magic used by the divine punishments, so a dampened player is shielded
	 * from every flavour of ability damage but still fights normally.
	 */
	public static boolean isPowerDamage(DamageSource source) {
		return source.is(POWER_DAMAGE);
	}

	static boolean isPowerDamageKey(ResourceKey<DamageType> key) {
		return POWER_MAGIC.equals(key);
	}
}
