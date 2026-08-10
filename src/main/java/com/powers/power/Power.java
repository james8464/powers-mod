package com.powers.power;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * One explicit superpower a player can be assigned. Three powers fill the
 * V/X/C slots; merely owning a power never applies an automatic effect.
 */
public record Power(
		Identifier id,
		Component name,
		Component description,
		int color,
		Ability ability,
		PowerAffinity affinity) {

	/** Most powers are universal; only explicitly authored powers are allegiance-locked. */
	public Power(Identifier id, Component name, Component description, int color, Ability ability) {
		this(id, name, description, color, ability, PowerAffinity.UNIVERSAL);
	}

	public String key() {
		return "power." + id.getNamespace() + "." + id.getPath();
	}
}
