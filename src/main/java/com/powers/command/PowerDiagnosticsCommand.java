package com.powers.command;

import com.mojang.brigadier.context.CommandContext;
import com.powers.audit.OperatorAudit;
import com.powers.ai.PerceptionSnapshotService;
import com.powers.companion.PrivateCompanionManager;
import com.powers.config.PowersConfigLoader;
import com.powers.config.PowerPolicyDiagnostics;
import com.powers.diagnostics.DiagnosticExport;
import com.powers.diagnostics.DiagnosticExportWriter;
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
import com.powers.player.PlayerEnergyHistory;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

/** Builds the bounded, non-sensitive administrator runtime report. */
final class PowerDiagnosticsCommand {
	private PowerDiagnosticsCommand() {
	}

	static int run(CommandContext<CommandSourceStack> context) {
		var server = context.getSource().getServer();
		RuntimeDiagnosticSnapshot snapshot = snapshot(server);
		for (String line : snapshot.lines()) send(context, line);
		send(context, "configValidation: " + PowersConfigLoader.validationReport().summary());
		var audit = OperatorAudit.snapshot();
		send(context, "operatorAudit: total=" + audit.total());
		for (var count : audit.counts()) {
			if (count.count() > 0) send(context, "operatorAudit action="
					+ count.action().name().toLowerCase(java.util.Locale.ROOT) + "; result="
					+ count.result().name().toLowerCase(java.util.Locale.ROOT) + "; count=" + count.count());
		}
		ServerPlayer inspectingPlayer = context.getSource().getPlayer();
		if (inspectingPlayer != null) {
			var energyHistory = PlayerEnergyHistory.snapshot(inspectingPlayer);
			send(context, energyHistory.tooltip());
			for (var value : energyHistory.breakdown()) {
				if (value.amount() > 0) send(context, "energySource="
						+ value.source().name().toLowerCase(java.util.Locale.ROOT)
						+ "; amount=" + value.amount());
			}
		}
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
		var spellIndexes = SpellFieldManager.spatialDiagnosticsByDimension();
		var artifactIndexes = ArtifactFieldManager.spatialDiagnosticsByDimension();
		var naturalAmethystIndexes = AmethystDampening.naturalIndexDiagnosticsByDimension();
		var wardIndexes = AmethystDampening.wardIndexDiagnosticsByDimension();
		var nameIndexes = NamedLivingTargetIndex.diagnosticsByDimension(server);
		var forceIndexes = LivingForceManager.diagnosticsByDimension();
		var ticketDiagnostics = TravelChunkLoader.diagnostics();
		var dimensions = new java.util.TreeSet<String>();
		server.getAllLevels().forEach(level -> dimensions.add(level.dimension().identifier().toString()));
		dimensions.addAll(spellIndexes.keySet());
		dimensions.addAll(artifactIndexes.keySet());
		dimensions.addAll(naturalAmethystIndexes.keySet());
		dimensions.addAll(wardIndexes.keySet());
		dimensions.addAll(nameIndexes.keySet());
		dimensions.addAll(forceIndexes.keySet());
		for (String dimension : dimensions) {
			var spellIndex = spellIndexes.getOrDefault(dimension,
					new com.powers.util.ChunkSpatialIndex.Diagnostics(0, 0, 0, 0, 0, 0, 0, 0));
			var artifactIndex = artifactIndexes.getOrDefault(dimension,
					new com.powers.util.ChunkSpatialIndex.Diagnostics(0, 0, 0, 0, 0, 0, 0, 0));
			var naturalAmethyst = naturalAmethystIndexes.getOrDefault(dimension,
					new com.powers.power.NaturalAmethystIndex.Diagnostics(0, 0, 0, 0, 0, 0, 0, 0));
			var wardIndex = wardIndexes.getOrDefault(dimension,
					new AmethystDampening.WardDiagnostics(0, 0, 0, 0, 0, 0, 0));
			var nameIndex = nameIndexes.getOrDefault(dimension,
					new com.powers.network.UniqueNameIndex.Diagnostics(0, 0, 0, 0, 0, 0, 0));
			var forceIndex = forceIndexes.getOrDefault(dimension,
					new LivingForceManager.Diagnostics(0, 0, 0, 0, 0, 0, 0, 0, 0));
			send(context, indexLine("spellFields", dimension, spellIndex.queries(),
					spellIndex.candidates(), spellIndex.misses(), spellIndex.staleRemovals(), 0L,
					spellIndex.estimatedBytes()));
			send(context, indexLine("artifactFields", dimension, artifactIndex.queries(),
					artifactIndex.candidates(), artifactIndex.misses(), artifactIndex.staleRemovals(), 0L,
					artifactIndex.estimatedBytes()));
			send(context, indexLine("amethystNatural", dimension, naturalAmethyst.queries(),
					naturalAmethyst.candidates(), naturalAmethyst.misses(), naturalAmethyst.staleRemovals(),
					naturalAmethyst.sectionScans(), naturalAmethyst.estimatedBytes()));
			send(context, indexLine("amethystWards", dimension, wardIndex.queries(),
					wardIndex.candidates(), wardIndex.misses(), wardIndex.staleRemovals(), 0L,
					wardIndex.estimatedBytes()));
			send(context, indexLine("namedTargets", dimension, nameIndex.queries(),
					nameIndex.candidates(), nameIndex.misses(), nameIndex.staleRemovals(), 0L,
					nameIndex.estimatedBytes()));
			send(context, indexLine("livingForces", dimension, forceIndex.queries(),
					forceIndex.candidates(), forceIndex.misses(), forceIndex.staleRemovals(), 0L,
					forceIndex.estimatedBytes()));
		}
		for (String line : PowerPolicyDiagnostics.lines(server)) send(context, line);
		send(context, "travelTickets=" + ticketDiagnostics.active() + "/" + ticketDiagnostics.limit()
				+ "; perDimensionLimit=" + ticketDiagnostics.perDimensionLimit()
				+ "; lastRefusal=" + ticketDiagnostics.lastRefusal());
		for (var ticket : ticketDiagnostics.tickets()) {
			send(context, "ticket owner=" + ticket.owner() + "; dimension=" + ticket.dimension()
					+ "; reason=" + ticket.reason() + "; deadline=" + ticket.deadline()
					+ "; state=" + ticket.state());
		}
		var delayedTasks = com.powers.PowersMod.delayedTasks();
		send(context, "delayedMagicTasks=" + delayedTasks.size());
		for (var task : delayedTasks) {
			send(context, "delayedTask owner=" + task.cancellationOwner()
					+ "; subject=" + task.subjectId() + "; dimension=" + task.dimensionId()
					+ "; purpose=" + task.purpose() + "; deadline=" + task.deadline());
		}
		var perception = PerceptionSnapshotService.diagnostics();
		send(context, com.powers.testing.network.PacketFaultController.diagnostics(server).line());
		send(context, "perceptionSnapshots: queries=" + perception.queries()
				+ "; hits=" + perception.cacheHits() + "; entityInspections="
				+ perception.inspections() + "; cachedCells=" + perception.cachedCells());
		var missingDimensions = PersistentDimensionDiagnostics.snapshot();
		send(context, "missingDimensions=" + missingDimensions.issues().size()
				+ "; droppedDistinct=" + missingDimensions.droppedDistinctKeys()
				+ "; orphanedRuinEvents=" + CelestialRuinManager.orphanedRitualCount(server));
		for (var issue : missingDimensions.issues()) {
			send(context, "missingDimension feature=" + issue.feature() + "; id="
					+ issue.dimension() + "; occurrences=" + issue.occurrences());
		}
		send(context, com.powers.progression.QuestCompletionTelemetry.diagnosticLine(server));
		if (context.getSource().getEntity() instanceof ServerPlayer player) {
			var testing = TestingOverrides.state(player.getUUID());
			send(context, "testing: energy=" + (testing.energyDisabled() ? "disabled" : "normal")
					+ "; cooldowns=" + (testing.cooldownsDisabled() ? "disabled" : "normal"));
		}
		return 1;
	}

	static int export(CommandContext<CommandSourceStack> context) {
		var server = context.getSource().getServer();
		DiagnosticExport document = DiagnosticExport.create(server.getTickCount(), snapshot(server),
				OperatorAudit.snapshot(), PowersConfigLoader.validationReport());
		DiagnosticExportWriter.Result result = DiagnosticExportWriter.write(
				server.getWorldPath(LevelResource.ROOT), document);
		if (!result.success()) {
			context.getSource().sendFailure(Component.literal(
					"Diagnostic export failed (" + result.failureReason() + ")."));
			return 0;
		}
		context.getSource().sendSuccess(() -> Component.literal(
				"Wrote redacted diagnostic aggregates to " + result.relativePath() + "."), false);
		return 1;
	}

	private static RuntimeDiagnosticSnapshot snapshot(net.minecraft.server.MinecraftServer server) {
		var work = ServerRuntimeMetrics.snapshot(server);
		var forces = LivingForceManager.diagnostics();
		int proxies = BodyProxyManager.activeProxyCount();
		int travelLoads = TravelChunkLoader.pendingRequestCount();
		int celestialChunks = CelestialRuinManager.forcedChunkCount(server);
		return new RuntimeDiagnosticSnapshot(
				MagicRuntime.global().activePresenceCount(), MagicRuntime.global().activePresenceCellCount(),
				SpellFieldManager.activeFieldCount(), SpellFieldManager.maxFieldWorkPerTick(),
				ArtifactFieldManager.activeFieldCount(), ArtifactGuardianSummons.indexedGuardianCount(),
				forces.indexedBlocks(), forces.activeClashes(), forces.auraCandidatesPerLevel(),
				forces.auraCandidatesPerPlayer(), proxies, travelLoads,
				CelestialRuinManager.activeRitualCount(server), celestialChunks + proxies + travelLoads * 9,
				work.packets(), work.particles(), work.entityInspections());
	}

	private static void send(CommandContext<CommandSourceStack> context, String line) {
		context.getSource().sendSuccess(
				() -> Component.literal(line).withStyle(ChatFormatting.AQUA), false);
	}

	private static String indexLine(String name, String dimension, long queries, long candidates, long misses,
			long staleRemovals, long fallbackScans, long estimatedBytes) {
		return "index=" + name + "; dimension=" + dimension + "; q=" + queries + "; candidates=" + candidates
				+ "; misses=" + misses + "; stale=" + staleRemovals
				+ "; fallbackScans=" + fallbackScans + "; memory~=" + estimatedBytes + "B";
	}
}
