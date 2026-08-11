package com.powers.command;

import com.mojang.brigadier.context.CommandContext;
import com.powers.companion.PrivateCompanionManager;
import com.powers.diagnostics.RuntimeDiagnosticSnapshot;
import com.powers.diagnostics.ServerRuntimeMetrics;
import com.powers.force.FactionInvasionManager;
import com.powers.force.ForceContainmentManager;
import com.powers.force.LivingForceManager;
import com.powers.magic.runtime.MagicRuntime;
import com.powers.magic.runtime.MagicRayCollisionRuntime;
import com.powers.mind.BodyProxyManager;
import com.powers.mind.PersistentDimensionDiagnostics;
import com.powers.network.NamedLivingTargetIndex;
import com.powers.power.AmethystDampening;
import com.powers.power.ConcordCastManager;
import com.powers.power.artifact.ArtifactFieldManager;
import com.powers.power.artifact.ArtifactGuardianSummons;
import com.powers.power.travel.TravelChunkLoader;
import com.powers.realm.RealmLandmarkConstruction;
import com.powers.spell.CelestialRuinManager;
import com.powers.spell.SpellFieldManager;
import com.powers.testing.TestingOverrides;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Builds the bounded, non-sensitive administrator runtime report. */
final class PowerDiagnosticsCommand {
	private PowerDiagnosticsCommand() {
	}

	static int run(CommandContext<CommandSourceStack> context) {
		var server = context.getSource().getServer();
		var work = ServerRuntimeMetrics.snapshot(server);
		var forces = LivingForceManager.diagnostics();
		int proxies = BodyProxyManager.activeProxyCount();
		int travelLoads = TravelChunkLoader.pendingRequestCount();
		int celestialChunks = CelestialRuinManager.forcedChunkCount(server);
		RuntimeDiagnosticSnapshot snapshot = new RuntimeDiagnosticSnapshot(
				MagicRuntime.global().activePresenceCount(), MagicRuntime.global().activePresenceCellCount(),
				SpellFieldManager.activeFieldCount(), SpellFieldManager.maxFieldWorkPerTick(),
				ArtifactFieldManager.activeFieldCount(), ArtifactGuardianSummons.indexedGuardianCount(),
				forces.indexedBlocks(), forces.activeClashes(), forces.auraCandidatesPerLevel(),
				forces.auraCandidatesPerPlayer(), proxies, travelLoads,
				CelestialRuinManager.activeRitualCount(server), celestialChunks + proxies + travelLoads * 9,
				work.packets(), work.particles(), work.entityInspections());
		for (String line : snapshot.lines()) send(context, line);
		send(context, "physicalRays=" + MagicRayCollisionRuntime.activeSegmentCount()
				+ "; rayCollisionsThisTick=" + MagicRayCollisionRuntime.collisionsThisTick()
				+ "; shadowSessions=" + PrivateCompanionManager.activeSessionCount()
				+ "; revealedShadowBodies=" + PrivateCompanionManager.activeRevealedBodyCount());
		send(context, PrivateCompanionManager.diagnostics().summary());
		var containment = ForceContainmentManager.diagnostics();
		var invasions = FactionInvasionManager.diagnostics();
		var concord = ConcordCastManager.diagnostics();
		send(context, "realmBuilds=" + RealmLandmarkConstruction.activeTaskCount()
				+ "; containment=" + containment.activeCeremonies()
				+ " (cap/tick " + containment.inspectionBudget() + ")"
				+ "; invasions=" + invasions.activeInvaders() + "/" + invasions.globalCap()
				+ "; concord=" + concord.recentCasts() + "/" + concord.coolingPairs());
		var spellIndex = SpellFieldManager.spatialDiagnostics();
		var artifactIndex = ArtifactFieldManager.spatialDiagnostics();
		var naturalAmethyst = AmethystDampening.naturalIndexDiagnostics();
		var wardIndex = AmethystDampening.wardIndexDiagnostics();
		var nameIndex = NamedLivingTargetIndex.diagnostics(server);
		var ticketDiagnostics = TravelChunkLoader.diagnostics();
		send(context, indexLine("spellFields", spellIndex.queries(), spellIndex.candidates(),
				spellIndex.misses(), spellIndex.staleRemovals(), 0L, spellIndex.estimatedBytes()));
		send(context, indexLine("artifactFields", artifactIndex.queries(), artifactIndex.candidates(),
				artifactIndex.misses(), artifactIndex.staleRemovals(), 0L, artifactIndex.estimatedBytes()));
		send(context, indexLine("amethystNatural", naturalAmethyst.queries(),
				naturalAmethyst.candidates(), naturalAmethyst.misses(), naturalAmethyst.staleRemovals(),
				naturalAmethyst.sectionScans(), naturalAmethyst.estimatedBytes()));
		send(context, indexLine("amethystWards", wardIndex.queries(), wardIndex.candidates(),
				wardIndex.misses(), wardIndex.staleRemovals(), 0L, wardIndex.estimatedBytes()));
		send(context, indexLine("namedTargets", nameIndex.queries(), nameIndex.candidates(),
				nameIndex.misses(), nameIndex.staleRemovals(), 0L, nameIndex.estimatedBytes()));
		send(context, indexLine("livingForces", forces.queries(), forces.candidates(),
				forces.misses(), forces.staleRemovals(), 0L, forces.estimatedBytes()));
		send(context, "travelTickets=" + ticketDiagnostics.active() + "/" + ticketDiagnostics.limit()
				+ "; perDimensionLimit=" + ticketDiagnostics.perDimensionLimit()
				+ "; lastRefusal=" + ticketDiagnostics.lastRefusal());
		for (var ticket : ticketDiagnostics.tickets()) {
			send(context, "ticket owner=" + ticket.owner() + "; dimension=" + ticket.dimension()
					+ "; reason=" + ticket.reason() + "; deadline=" + ticket.deadline()
					+ "; state=" + ticket.state());
		}
		var missingDimensions = PersistentDimensionDiagnostics.snapshot();
		send(context, "missingDimensions=" + missingDimensions.issues().size()
				+ "; droppedDistinct=" + missingDimensions.droppedDistinctKeys()
				+ "; orphanedRuinEvents=" + CelestialRuinManager.orphanedRitualCount(server));
		for (var issue : missingDimensions.issues()) {
			send(context, "missingDimension feature=" + issue.feature() + "; id="
					+ issue.dimension() + "; occurrences=" + issue.occurrences());
		}
		if (context.getSource().getEntity() instanceof ServerPlayer player) {
			var testing = TestingOverrides.state(player.getUUID());
			send(context, "testing: energy=" + (testing.energyDisabled() ? "disabled" : "normal")
					+ "; cooldowns=" + (testing.cooldownsDisabled() ? "disabled" : "normal"));
		}
		return 1;
	}

	private static void send(CommandContext<CommandSourceStack> context, String line) {
		context.getSource().sendSuccess(
				() -> Component.literal(line).withStyle(ChatFormatting.AQUA), false);
	}

	private static String indexLine(String name, long queries, long candidates, long misses,
			long staleRemovals, long fallbackScans, long estimatedBytes) {
		return "index=" + name + "; q=" + queries + "; candidates=" + candidates
				+ "; misses=" + misses + "; stale=" + staleRemovals
				+ "; fallbackScans=" + fallbackScans + "; memory~=" + estimatedBytes + "B";
	}
}
