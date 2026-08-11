package com.powers.client.fx;

import net.minecraft.client.Minecraft;

/** Reads vanilla accessibility controls so magic respects the player's preferences. */
public final class FxAccessibility {
	private FxAccessibility() {
	}

	public static double effectScale(Minecraft client) {
		return Math.clamp(client.options.screenEffectScale().get(), 0.0, 1.0) * particleScale(client);
	}

	public static double particleScale(Minecraft client) {
		return switch (client.options.particles().get()) {
			case ALL -> 1.0;
			case DECREASED -> 0.55;
			case MINIMAL -> 0.25;
		};
	}

	public static boolean reducedMotion(Minecraft client) {
		return effectScale(client) < 0.45;
	}
}
