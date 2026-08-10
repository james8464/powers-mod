package com.powers.diagnostics;

import net.minecraft.server.MinecraftServer;

import java.util.Map;
import java.util.WeakHashMap;

/** Per-server live work counters used by {@code /powers diagnose}. */
public final class ServerRuntimeMetrics {
	private static final Map<MinecraftServer, TickWorkMetrics> METRICS = new WeakHashMap<>();

	private ServerRuntimeMetrics() {
	}

	public static void recordParticles(MinecraftServer server, long tick, int amount) {
		metrics(server).recordParticles(tick, amount);
	}

	public static void recordPacket(MinecraftServer server, long tick) {
		metrics(server).recordPackets(tick, 1);
	}

	public static void recordEntityInspections(MinecraftServer server, long tick, int amount) {
		metrics(server).recordEntityInspections(tick, amount);
	}

	public static TickWorkMetrics.Snapshot snapshot(MinecraftServer server) {
		return metrics(server).snapshot(server.getTickCount());
	}

	public static void clear() {
		METRICS.clear();
	}

	private static TickWorkMetrics metrics(MinecraftServer server) {
		return METRICS.computeIfAbsent(server, ignored -> new TickWorkMetrics());
	}
}
