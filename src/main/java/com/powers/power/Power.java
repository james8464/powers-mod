package com.powers.power;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * A single selectable power: one active ability, optionally backed by one or
 * more permanent passive effects. Three powers fill the V/X/C slots; the
 * slot keybind triggers the power's ability.
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
