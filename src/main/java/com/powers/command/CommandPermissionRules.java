package com.powers.command;

/** Pure mapping from the configurable operator level to Minecraft's permission tiers. */
public final class CommandPermissionRules {
	private CommandPermissionRules() {
	}

	public static int tier(int configuredLevel) {
		return Math.clamp(configuredLevel, 0, 4);
	}
}
