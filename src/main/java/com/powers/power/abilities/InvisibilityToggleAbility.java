package com.powers.power.abilities;

import com.powers.PowersMod;
import com.powers.player.PlayerPowers;
import com.powers.power.ToggleAbility;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class InvisibilityToggleAbility extends ToggleAbility {
	public InvisibilityToggleAbility() {
		super(PowersMod.id("invisibility"),
				Component.translatable("ability.powers.invisibility"));
	}

	@Override
	public boolean activateToggleOn(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		player.setInvisible(true);
		if (player.level() instanceof net.minecraft.server.level.ServerLevel level) {
			com.powers.fx.PowerFx.burst(level, player.position(), net.minecraft.core.particles.ParticleTypes.SMOKE, 18, 0.5, 0.02);
			com.powers.fx.PowerFx.sound(level, player.position(), net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT, 0.7f, 1.6f);
		}
		return true;
	}

	@Override
	public void activateToggleOff(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		player.setInvisible(false);
		if (player.level() instanceof net.minecraft.server.level.ServerLevel level) {
			com.powers.fx.PowerFx.burst(level, player.position(), net.minecraft.core.particles.ParticleTypes.PORTAL, 18, 0.5, 0.02);
		}
	}

	@Override
	public void tickActive(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		if (!player.isInvisible()) {
			player.setInvisible(true);
		}
	}
}
