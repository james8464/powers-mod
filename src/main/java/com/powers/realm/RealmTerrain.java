package com.powers.realm;

import com.powers.util.LoadedChunks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.Heightmap;

/** Existing-flat/new-noise compatible height resolution for fixed realm sites. */
public final class RealmTerrain {
	private static final int PROVISIONAL_Y = 64;

	private RealmTerrain() { }

	/** Safe pre-load Y used only to validate bounds and request the X/Z chunk. */
	public static int provisionalArrivalY(ServerLevel level) {
		return Math.clamp(PROVISIONAL_Y, level.getMinY() + 1, level.getMaxY() - 2);
	}

	/** First air block above the loaded terrain; old flat chunks resolve to minY+1. */
	public static int arrivalY(ServerLevel level, int x, int z) {
		BlockPos probe = new BlockPos(x, provisionalArrivalY(level), z);
		if (!LoadedChunks.contains(level, probe)) return provisionalArrivalY(level);
		return Math.clamp(level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z),
				level.getMinY() + 1, level.getMaxY() - 2);
	}

	public static int floorY(ServerLevel level, int x, int z) {
		return arrivalY(level, x, z) - 1;
	}
}
