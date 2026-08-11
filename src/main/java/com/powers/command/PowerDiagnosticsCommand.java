package com.powers.command;

import com.mojang.brigadier.context.CommandContext;
import com.powers.companion.PrivateCompanionManager;
import com.powers.diagnostics.RuntimeDiagnosticSnapshot;
import com.powers.diagnostics.ServerRuntimeMetrics;
import com.powers.force.FactionInvasionManager;
import com.powers.force.ForceContainmentManager;
import com.powers.force.LivingForceManager;
import com.powers.magic.runtime.MagicRuntime;
import com.powers.mind.BodyProxyManager;
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
		send(context, "shadow sessions: " + PrivateCompanionManager.activeSessionCount());
		var containment = ForceContainmentManager.diagnostics();
		var invasions = FactionInvasionManager.diagnostics();
		var concord = ConcordCastManager.diagnostics();
		send(context, "realmBuilds=" + RealmLandmarkConstruction.activeTaskCount()
				+ "; containment=" + containment.activeCeremonies()
				+ " (cap/tick " + containment.inspectionBudget() + ")"
				+ "; invasions=" + invasions.activeInvaders() + "/" + invasions.globalCap()
				+ "; concord=" + concord.recentCasts() + "/" + concord.coolingPairs());
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
}
