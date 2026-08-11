package com.powers.diagnostics;

import com.powers.companion.PrivateCompanionManager;
import com.powers.knowledge.MagicAttemptJournal;
import com.powers.magic.runtime.MagicRuntime;
import com.powers.mind.BodyProxyManager;
import com.powers.power.travel.TravelChunkLoader;
import com.powers.spell.CelestialRuinManager;
import com.powers.spell.SpellFieldManager;
import net.minecraft.server.MinecraftServer;

import java.util.Map;

/** Collects fixed aggregate counts for crash reports without player or world identifiers. */
public final class CrashReportDiagnostics {
	private CrashReportDiagnostics() {
	}

	public static CrashDiagnosticSection capture(MinecraftServer server) {
		Map<String, Integer> counts = Map.of(
				"magic_presences", MagicRuntime.global().activePresenceCount(),
				"spell_fields", SpellFieldManager.activeFieldCount(),
				"body_proxies", BodyProxyManager.activeProxyCount(),
				"travel_requests", TravelChunkLoader.pendingRequestCount(),
				"private_companions", PrivateCompanionManager.activeSessionCount(),
				"celestial_rituals", CelestialRuinManager.activeRitualCount(server));
		return CrashDiagnosticSection.create(counts,
				MagicAttemptJournal.global().latestGlobalFailure().orElse(null));
	}
}
