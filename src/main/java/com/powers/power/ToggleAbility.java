package com.powers.power;

import com.powers.player.PlayerPowers;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/**
 * Base class for toggle abilities: pressing the key once turns the power on
 * and pressing it again turns it off. Toggle state is stored in the player's
 * power data, survives logins and is synced to the client for the HUD.
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
