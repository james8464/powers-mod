package com.powers.power.crystals;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/** Pure dimension-transition policy for tracked remote camera entities. */
public final class DreamwalkingRules {
	private DreamwalkingRules() {
	}

	public static boolean mustTravel(ResourceKey<Level> viewer, ResourceKey<Level> host) {
		return viewer != null && host != null && !viewer.equals(host);
	}
}
