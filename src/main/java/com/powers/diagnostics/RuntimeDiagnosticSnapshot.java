package com.powers.diagnostics;

import java.util.List;

/** Compact immutable report rendered by the administrative diagnostics command. */
public record RuntimeDiagnosticSnapshot(
		int activeMagic,
		int magicCells,
		int spellFields,
		int spellFieldWorkCap,
		int artifactFields,
		int guardians,
		int forceBlocks,
		int forceClashes,
		int auraLevelCap,
		int auraPlayerCap,
		int proxies,
		int travelRequests,
		int celestialEvents,
		int forcedChunks,
		int packets,
		int particles,
		int entityInspections) {
	public List<String> lines() {
		return List.of(
				"magic=" + activeMagic + "/" + magicCells
						+ " cells; spellFields=" + spellFields + " (cap/tick " + spellFieldWorkCap + ")"
						+ "; artifactFields=" + artifactFields,
				"forces=" + forceBlocks + "; clashes=" + forceClashes
						+ "; auraCaps=" + auraPlayerCap + "/player," + auraLevelCap + "/level"
						+ "; guardians=" + guardians,
				"proxies=" + proxies + "; travelLoads=" + travelRequests
						+ "; celestialEvents=" + celestialEvents + "; forcedChunks=" + forcedChunks,
				"lastTick: packets=" + packets + "; particles=" + particles
						+ "; entityInspections=" + entityInspections);
	}
}
