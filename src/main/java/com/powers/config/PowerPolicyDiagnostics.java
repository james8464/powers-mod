package com.powers.config;

import net.minecraft.server.MinecraftServer;

import java.util.Comparator;
import java.util.List;

/** Builds deterministic operator-safe lines for every loaded policy scope. */
public final class PowerPolicyDiagnostics {
	private PowerPolicyDiagnostics() {
	}

	public static List<String> lines(MinecraftServer server) {
		String world = server.getWorldData().getLevelName();
		return java.util.stream.StreamSupport.stream(server.getAllLevels().spliterator(), false)
				.sorted(Comparator.comparing(level -> level.dimension().identifier().toString()))
				.map(level -> ResolvedPowerPolicy.resolve(level).diagnosticLine(
						world, level.dimension().identifier().toString()))
				.toList();
	}
}
