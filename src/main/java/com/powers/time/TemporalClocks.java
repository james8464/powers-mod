package com.powers.time;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

/** The only conversion boundary between Minecraft counters and typed POWERS clocks. */
public final class TemporalClocks {
	private TemporalClocks() {
	}

	public static ControlTick control(MinecraftServer server) {
		return ControlTick.at(server.getTickCount());
	}

	public static WorldTick world(ServerLevel level) {
		return WorldTick.at(level.getGameTime());
	}

	public static boolean worldAdvances(MinecraftServer server) {
		return worldAdvances(server.tickRateManager().isFrozen());
	}

	static boolean worldAdvances(boolean vanillaFrozen) {
		return !vanillaFrozen;
	}
}
