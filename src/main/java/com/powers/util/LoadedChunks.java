package com.powers.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/** Non-generating chunk-presence checks for server-authoritative validation. */
public final class LoadedChunks {
	private LoadedChunks() {
	}

	public static boolean contains(ServerLevel level, BlockPos pos) {
		return level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) != null;
	}
}
