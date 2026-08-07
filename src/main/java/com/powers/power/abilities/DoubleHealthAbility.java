package com.powers.power.abilities;

import com.powers.PowersMod;
import com.powers.player.PlayerPowers;
import com.powers.power.ToggleAbility;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public class DoubleHealthAbility extends ToggleAbility {
	public DoubleHealthAbility() {
		super(PowersMod.id("double_health"), Component.translatable("ability.powers.double_health"));
	}

	@Override
	public boolean activateToggleOn(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		player.addEffect(new MobEffectInstance(MobEffects.HEALTH_BOOST, -1, 4, false, false, true));
		player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + 20.0f));
		if (player.level() instanceof net.minecraft.server.level.ServerLevel level) {
			com.powers.fx.PowerFx.spiral(level, player.position(), 0.8, 2.0, 0xFF1744, 20, 0);
		}
		return true;
	}

	@Override
	public void activateToggleOff(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		player.removeEffect(MobEffects.HEALTH_BOOST);
		player.setHealth(Math.min(player.getMaxHealth(), player.getHealth()));
	}

	@Override
	public void tickActive(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		// Re-assert the boost so a milk bucket or effect clear cannot silently
		// strip the doubled health while the toggle is still on.
		if (!player.hasEffect(MobEffects.HEALTH_BOOST)) {
			player.addEffect(new MobEffectInstance(MobEffects.HEALTH_BOOST, -1, 4, false, false, true));
		}
	}
}
