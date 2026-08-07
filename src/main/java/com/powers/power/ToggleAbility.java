package com.powers.power;

import com.powers.player.PlayerPowers;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/**
 * A power the player flips on and off with the key. The on/off state
 * lives in the player's power data, so it survives logins and syncs to
 * the client for the HUD
 */
public abstract class ToggleAbility extends Ability {
	protected ToggleAbility(Identifier id, Component name) {
		super(id, name, 0, false);
	}

	@Override
	public final boolean isToggle() {
		return true;
	}

	@Override
	public abstract boolean activateToggleOn(ServerPlayer player, PlayerPowers.PlayerPowersData data);

	@Override
	public void activateToggleOff(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
	}

	@Override
	public void tickActive(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
	}
}
