package com.powers.power;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * One superpower a player can be assigned: an active ability, possibly
 * with permanent passive effects layered on. Three of these fill the
 * V/X/C slots, and each slot key fires its power
 */
public record Power(
		Identifier id,
		Component name,
		Component description,
		int color,
		List<PassiveEffect> passives,
		Ability ability) {

	public String key() {
		return "power." + id.getNamespace() + "." + id.getPath();
	}
}
