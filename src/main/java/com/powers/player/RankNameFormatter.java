package com.powers.player;

import net.minecraft.network.chat.Component;

/** Builds a rank-aware name while preserving Minecraft's existing name component. */
public final class RankNameFormatter {
	private RankNameFormatter() {
	}

	/** Prepends a styled rank prefix without flattening team, hover, or click styling. */
	public static Component decorate(Component prefix, Component vanillaName) {
		return decorate(true, prefix, vanillaName);
	}

	/** Returns the original component when disabled, preserving other formatting mods exactly. */
	public static Component decorate(boolean enabled, Component prefix, Component vanillaName) {
		if (!enabled) return vanillaName;
		if (prefix.getString().isEmpty()) {
			return vanillaName;
		}
		return prefix.copy().append(vanillaName);
	}
}
