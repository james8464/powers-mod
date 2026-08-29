package com.powers.time;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

/** The only conversion boundary between Minecraft counters and typed POWERS clocks. */
public final class TemporalClocks {
	private static final java.util.Map<MinecraftServer, ControlTickCounter> CONTROL_COUNTERS =
			java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());

	private TemporalClocks() {
	}

	public static ControlTick control(MinecraftServer server) {
		return CONTROL_COUNTERS.computeIfAbsent(server, ignored -> new ControlTickCounter())
				.observe(server.getTickCount());
	}

	public static WorldTick world(ServerLevel level) {
		return WorldTick.at(level.getGameTime());
	}

	public static boolean worldAdvances(MinecraftServer server) {
		return worldAdvances(server.tickRateManager().isFrozen());
	}

	public static boolean worldAdvances(MinecraftServer server, TemporalSubsystem subsystem) {
		requireWorld(subsystem);
		return worldAdvances(server);
	}

	public static boolean worldPulse(MinecraftServer server, ServerLevel level, long interval) {
		return worldPulse(server.tickRateManager().isFrozen(), world(level), interval);
	}

	public static boolean worldPulse(MinecraftServer server, ServerLevel level, long interval,
			TemporalSubsystem subsystem) {
		requireWorld(subsystem);
		return worldPulse(server, level, interval);
	}

	static boolean worldPulse(boolean vanillaFrozen, WorldTick tick, long interval) {
		if (interval <= 0L) {
			throw new IllegalArgumentException("interval must be positive");
		}
		return !vanillaFrozen && tick.value() % interval == 0L;
	}

	static boolean worldAdvances(boolean vanillaFrozen) {
		return !vanillaFrozen;
	}

	private static void requireWorld(TemporalSubsystem subsystem) {
		if (subsystem == null || subsystem.clock() != TemporalClockKind.WORLD) {
			throw new IllegalArgumentException("Subsystem is not owned by the world clock");
		}
	}
}
