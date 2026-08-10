package com.powers.power.abilities;

import com.powers.PowersMod;
import com.powers.PowerStatusEffects;
import com.powers.player.PlayerPowers;
import com.powers.power.ToggleAbility;
import com.powers.power.ToggleKeyRules;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/**
 * Invisibility: a toggle that lets you vanish and reappear. Turning on
 * hides you in a puff of smoke, turning off brings you back with a flash.
 */
public class InvisibilityToggleAbility extends ToggleAbility {
	public InvisibilityToggleAbility() {
		super(PowersMod.id("invisibility"),
				Component.translatable("ability.powers.invisibility"));
	}

	@Override
	public boolean activateToggleOn(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		player.addEffect(ownedEffect());
		if (player.level() instanceof net.minecraft.server.level.ServerLevel level) {
			// smoke burst hides the spot where you stood
			com.powers.fx.PowerFx.burst(level, player.position(), net.minecraft.core.particles.ParticleTypes.SMOKE, 18, 0.5, 0.02);
			com.powers.fx.PowerFx.sound(level, player.position(), net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT, 0.7f, 1.6f);
		}
		return true;
	}

	@Override
	public void activateToggleOff(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		MobEffectInstance current = player.getEffect(MobEffects.INVISIBILITY);
		if (isOwnedEffect(current)) player.removeEffect(MobEffects.INVISIBILITY);
		if (player.level() instanceof net.minecraft.server.level.ServerLevel level) {
			com.powers.fx.PowerFx.burst(level, player.position(), net.minecraft.core.particles.ParticleTypes.PORTAL, 18, 0.5, 0.02);
		}
	}

	@Override
	public void tickActive(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		// Reapply only our unmistakable amplifier-255 effect if milk or a command
		// removed it while the toggle remains server-owned.
		if (!isOwnedEffect(player.getEffect(MobEffects.INVISIBILITY))) player.addEffect(ownedEffect());
		if (player.level() instanceof net.minecraft.server.level.ServerLevel level) {
			boolean veilFocus = scaling(player).unlockedVariants().contains("afterimage");
			int interval = veilFocus ? 40 : 20;
			if (level.getServer().getTickCount() % interval == 0) {
				// Invisibility leaves a faint, readable magical residue; veil ranks
				// reduce its frequency but never remove counterplay completely.
				com.powers.fx.PowerFx.burst(level, player.position().add(0, 1, 0),
						net.minecraft.core.particles.ParticleTypes.REVERSE_PORTAL, 2, 0.25, 0.01);
			}
		}
	}

	static MobEffectInstance ownedEffect() {
		return PowerStatusEffects.hidden(MobEffects.INVISIBILITY,
				Integer.MAX_VALUE, 255, false, false);
	}

	private static boolean isOwnedEffect(MobEffectInstance effect) {
		return effect != null && effect.getAmplifier() == 255
				&& !effect.isVisible() && !effect.showIcon();
	}

	/** Breaks power-owned invisibility after the player successfully harms a target. */
	public static void breakOnAttack(ServerPlayer attacker) {
		reveal(attacker);
	}

	/** Reveals only POWERS-owned invisibility, preserving unrelated invisible states. */
	public static boolean reveal(ServerPlayer player) {
		PlayerPowers.PlayerPowersData data = PlayerPowers.get(player);
		var abilityId = PowersMod.id("invisibility");
		var ownedKeys = data.getActiveToggles().stream()
				.filter(key -> ToggleKeyRules.ownsAbility(key, abilityId)).toList();
		if (ownedKeys.isEmpty()) return false;
		new InvisibilityToggleAbility().activateToggleOff(player, data);
		ownedKeys.forEach(key -> data.setToggleActive(player, key, false));
		if (player.level() instanceof net.minecraft.server.level.ServerLevel level) {
			com.powers.fx.PowerFx.rune(level, player.position().add(0, 1, 0), 1.1, 0x6E7180, 18, Math.PI);
		}
		return true;
	}
}
