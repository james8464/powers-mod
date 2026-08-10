package com.powers.player;

import net.minecraft.network.chat.Component;

/** Access to the rank prefix tracked by every player entity on both logical sides. */
public interface RankDisplayData {
	/** Returns the prefix currently synchronised to clients. */
	Component powers$getRankPrefix();

	/** Updates the prefix and marks the player's tracked entity data dirty. */
	void powers$setRankPrefix(Component prefix);
}
