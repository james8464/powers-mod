package com.powers;

import com.powers.companion.DialogueProviderRuntime;
import com.powers.companion.PrivateCompanionManager;
import com.powers.config.PowersConfigLoader;
import com.powers.diagnostics.ServerRuntimeMetrics;
import com.powers.force.FactionInvasionManager;
import com.powers.force.ForceContainmentManager;
import com.powers.force.LivingForceManager;
import com.powers.forge.CrucibleWeaponRuntime;
import com.powers.item.ArtifactInventoryRuntime;
import com.powers.knowledge.KnowledgeRemoteProviderRuntime;
import com.powers.magic.runtime.MagicRuntime;
import com.powers.magic.runtime.PhysicalMagicPresences;
import com.powers.mind.BodyProxyManager;
import com.powers.network.MagicFxPackets;
import com.powers.network.CompanionPackets;
import com.powers.network.NamedLivingTargetIndex;
import com.powers.network.PowersPackets;
import com.powers.player.PlayerGuide;
import com.powers.player.PlayerPowers;
import com.powers.player.SkillSystem;
import com.powers.power.AmethystDampening;
import com.powers.power.ConcordCastManager;
import com.powers.power.PowerAbilityRuntime;
import com.powers.power.crystals.CrystalPowerRegistry;
import com.powers.power.travel.TravelChunkLoader;
import com.powers.protection.ConsentOverrideRuntime;
import com.powers.realm.RealmConfinementManager;
import com.powers.realm.RealmEventManager;
import com.powers.realm.RealmMindscapeManager;
import com.powers.spell.CelestialRuinManager;
import com.powers.spell.SpellCastingManager;
import com.powers.spell.SpellFieldManager;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Wires Fabric server callbacks to focused runtime owners in a stable order. */
final class PowersServerLifecycle {
	private PowersServerLifecycle() {
	}

	static void initialize() {
		ServerMessageEvents.ALLOW_CHAT_MESSAGE.register((message, sender, parameters) ->
				!PrivateCompanionManager.handleChat(sender, message.signedContent()));
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
				onJoin(handler.getPlayer()));
		ServerPlayerEvents.AFTER_RESPAWN.register(PowersServerLifecycle::afterRespawn);
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
				onDisconnect(server, handler.getPlayer()));
		ServerLifecycleEvents.SERVER_STOPPING.register(BodyProxyManager::returnAll);
		ServerLifecycleEvents.SERVER_STOPPED.register(PowersServerLifecycle::onServerStopped);
		ServerTickEvents.END_SERVER_TICK.register(PowersServerLifecycle::tick);
	}

	private static void onJoin(ServerPlayer player) {
		PlayerPowerTicker.migrateLegacyRealmGamemode(player);
		if (!PowersConfigLoader.get().persistCooldowns()) {
			PlayerPowers.get(player).clearCooldowns();
		}
		BodyProxyManager.recoverOnJoin(player);
		PlayerPowers.get(player).assignRandom(player, false);
		PlayerGuide.giveIfNeeded(player);
		SkillSystem.syncPathVisibility(player);
		SkillSystem.refresh(player);
		PowersPackets.syncTo(player);
	}

	private static void afterRespawn(ServerPlayer oldPlayer, ServerPlayer newPlayer, boolean alive) {
		PlayerPowerTicker.migrateLegacyRealmGamemode(newPlayer);
		MagicRuntime.global().clearOwner(oldPlayer.getUUID());
		PowerAbilityRuntime.afterRespawn(newPlayer.level().getServer(), oldPlayer, newPlayer);
		PlayerPowerTicker.forget(newPlayer.getUUID());
		SkillSystem.clear(newPlayer.getUUID());
		SpellCastingManager.clear(oldPlayer);
		ArtifactInventoryRuntime.forget(oldPlayer);
		CrucibleWeaponRuntime.forget(oldPlayer.getUUID());
		TravelChunkLoader.cancel(oldPlayer.getUUID());
		PowersPackets.forget(oldPlayer);
		BodyProxyManager.discardOnDeath(newPlayer);
		if (!alive) {
			PowerAbilityRuntime.deactivateToggles(newPlayer);
			ArtifactInventoryRuntime.stopAllToggles(newPlayer);
			RealmConfinementManager.restoreAfterDeath(oldPlayer, newPlayer);
		}
		SkillSystem.syncPathVisibility(newPlayer);
		SkillSystem.refresh(newPlayer);
		PowersPackets.syncTo(newPlayer);
	}

	private static void onDisconnect(MinecraftServer server, ServerPlayer player) {
		if (!PowersConfigLoader.get().persistCooldowns()) {
			PlayerPowers.get(player).clearCooldowns();
		}
		MagicRuntime.global().clearOwner(player.getUUID());
		PlayerPowerTicker.forget(player.getUUID());
		SkillSystem.clear(player.getUUID());
		PowerAbilityRuntime.onDisconnect(server, player);
		CrystalPowerRegistry.clearSelections(player.getUUID());
		BodyProxyManager.returnToBody(player);
		SpellCastingManager.clear(player);
		AmethystDampening.forget(player);
		ArtifactInventoryRuntime.forget(player);
		CrucibleWeaponRuntime.forget(player.getUUID());
		PrivateCompanionManager.forget(player);
		KnowledgeRemoteProviderRuntime.forget(player.getUUID());
		TravelChunkLoader.cancel(player.getUUID());
		ConcordCastManager.forget(player.getUUID());
		RealmEventManager.forget(player.getUUID());
		PowersPackets.forget(player);
	}

	private static void onServerStopped(MinecraftServer server) {
		MagicRuntime.global().clearAll();
		ServerMagicScheduler.clear();
		PlayerPowerTicker.clear();
		SkillSystem.clearAll();
		AmethystDampening.clearAll();
		LivingForceManager.clearAll();
		ForceContainmentManager.clear();
		FactionInvasionManager.clear();
		ConcordCastManager.clear();
		PowerAbilityRuntime.onServerStopped(server);
		SpellCastingManager.clearAll();
		SpellFieldManager.clearAll();
		CelestialRuinManager.clearAll();
		RealmMindscapeManager.clearAll();
		ArtifactInventoryRuntime.clear();
		CrucibleWeaponRuntime.clear();
		PrivateCompanionManager.clear();
		DialogueProviderRuntime.clear();
		KnowledgeRemoteProviderRuntime.clear();
		com.powers.fx.PowerFx.clearBudgets();
		PowersPackets.clearSyncCache();
		CompanionPackets.clearBudgets();
		MagicFxPackets.clear();
		TravelChunkLoader.clear();
		NamedLivingTargetIndex.clearAll();
		ServerRuntimeMetrics.clear();
		PhysicalMagicPresences.clear();
		ConsentOverrideRuntime.clear();
	}

	private static void tick(MinecraftServer server) {
		int tick = server.getTickCount();
		MagicRuntime.global().tick(tick);
		PhysicalMagicPresences.tick(tick);
		PlayerTickCoordinator.tick(server, tick);
		ArtifactInventoryRuntime.tickServer(server);
		PowerAbilityRuntime.tick(server);
		CrystalPowerRegistry.tick(server);
		BodyProxyManager.tickAll();
		SpellCastingManager.tick(server);
		SpellFieldManager.tick(server);
		CelestialRuinManager.tick(server);
		RealmMindscapeManager.tick(server);
		LivingForceManager.tick(server);
		ForceContainmentManager.tick(server);
		FactionInvasionManager.tick(server);
		PowerAbilityRuntime.tickTeleportMarking();
		ServerMagicScheduler.tick(tick);
	}
}
