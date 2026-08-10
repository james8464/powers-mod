package com.powers;

import com.powers.command.PowerCommand;
import com.powers.mind.BodyProxyManager;
import com.powers.magic.runtime.MagicRuntime;
import com.powers.config.PowersConfigLoader;
import com.powers.fx.GodlyPunishment;
import com.powers.force.LivingForceManager;
import com.powers.network.PowersPackets;
import com.powers.network.MagicFxPackets;
import com.powers.player.PlayerPowers;
import com.powers.player.PlayerTickCadence;
import com.powers.progression.RankGraphRegistry;
import com.powers.progression.PowerScalingService;
import com.powers.power.Ability;
import com.powers.power.PassiveEffect;
import com.powers.power.Power;
import com.powers.power.PowerAbilityRuntime;
import com.powers.power.PowerEnergy;
import com.powers.power.PowerRegistry;
import com.powers.power.AmethystDampening;
import com.powers.power.state.PowerEntityState;
import com.powers.power.travel.TravelChunkLoader;
import com.powers.player.SkillSystem;
import com.powers.power.crystals.CrystalPowerRegistry;
import com.powers.util.PowerMessages;
import com.powers.util.ScheduledTaskQueue;
import com.powers.spell.SpellCastingManager;
import com.powers.spell.SpellFieldManager;
import com.powers.spell.CelestialRuinManager;
import com.powers.realm.RealmMindscapeManager;
import com.powers.realm.RealmConfinementManager;
import com.powers.loot.PowersLoot;
import com.powers.item.ArtifactInventoryRuntime;
import com.powers.item.ArtifactWeaponManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Fabric entry point that registers POWERS systems and coordinates their
 * server lifecycle. Persistent player state belongs to attachments; this
 * class owns only ephemeral per-session bookkeeping.
 */
public class PowersMod implements ModInitializer {
	public static final String MOD_ID = "powers";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	// passives are re-applied every 100 ticks (5 seconds) so they never expire
	private static final int PASSIVE_REFRESH_TICKS = 100;

	// the signature a summoned storm carries: which realm's weather it echoes
	public enum StormTheme { NONE, DARK, LIGHT }

	// whether each player was asleep last tick, so waking up can refund energy
	private static final Map<UUID, Boolean> WAS_SLEEPING = new HashMap<>();

	@Override
	public void onInitialize() {
		PowersConfigLoader.initialize();
		PowerEntityState.initialize();
		RankGraphRegistry.initialize();
		PowersEffects.initialize();
		PowersSounds.initialize();
		PowersParticles.initialize();
		PowersDataComponents.initialize();
		PowersEntities.initialize();
		PowerRegistry.initialize();
		PowersItems.initialize();
		PowersWeapons.initialize();
		PowersBlocks.initialize();
		LivingForceManager.initialize();
		ImportedPackItems.initialize();
		PowersLoot.initialize();
		PowersCreativeTab.initialize();
		CrystalPowerRegistry.initialize();
		ArtifactWeaponManager.initialize();
		PowersPackets.initialize();
		PowerCommand.register();
		PowerCombatEvents.register();
		LOGGER.info("Magic collision kernel loaded: {} actions, {} exhaustive interactions",
				MagicRuntime.catalogue().definitions().size(), MagicRuntime.global().interactionCount());
		// SkillSystem sets the player's visible display name. Vanilla signed chat
		// therefore carries the rank without cancelling, stripping, or rebuilding
		// the authenticated message as an unsigned system message.

		// first join rolls three random powers that stick with the player for good
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayer player = handler.getPlayer();
			BodyProxyManager.recoverOnJoin(player);
			PlayerPowers.get(player).assignRandom(player, false);
			SkillSystem.syncPathVisibility(player);
			SkillSystem.refresh(player);
			PowersPackets.syncTo(player);
		});
		// a respawned player is a brand new entity: the attachments come across
		// with it, but the passive effects, name plate and client hud all have
		// to be rebuilt straight away instead of waiting for the next refresh
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
			MagicRuntime.global().clearOwner(oldPlayer.getUUID());
			PowerAbilityRuntime.afterRespawn(newPlayer.level().getServer(), oldPlayer.getUUID());
			WAS_SLEEPING.remove(newPlayer.getUUID());
			SkillSystem.clear(newPlayer.getUUID());
			// Vanilla respawning begins outside a mindscape. Discard the dead
			// proxy snapshot, then confinement may return the new body to its realm.
			if (!alive) {
				BodyProxyManager.discardOnDeath(newPlayer);
				PlayerPowers.get(newPlayer).setPreviousGameMode(null);
				RealmConfinementManager.restoreAfterDeath(oldPlayer, newPlayer);
			}
			refreshPassives(newPlayer);
			SkillSystem.syncPathVisibility(newPlayer);
			SkillSystem.refresh(newPlayer);
			PowersPackets.syncTo(newPlayer);
		});
		// Drop ephemeral runtime state when someone leaves. Persistent cooldowns,
		// anchors, and owned flag snapshots deliberately stay on the player.
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			ServerPlayer player = handler.getPlayer();
			MagicRuntime.global().clearOwner(player.getUUID());
			WAS_SLEEPING.remove(player.getUUID());
			SkillSystem.clear(player.getUUID());
			PowerAbilityRuntime.onDisconnect(server, player);
			CrystalPowerRegistry.clearSelections(player.getUUID());
			BodyProxyManager.returnToBody(player);
			SpellCastingManager.clear(player);
			AmethystDampening.forget(player);
			ArtifactInventoryRuntime.forget(player);
			TravelChunkLoader.cancel(player.getUUID());
			PowersPackets.forget(player);
		});
		ServerLifecycleEvents.SERVER_STOPPING.register(BodyProxyManager::returnAll);
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			MagicRuntime.global().clearAll();
			ServerMagicScheduler.clear();
			WAS_SLEEPING.clear();
			SkillSystem.clearAll();
			AmethystDampening.clearAll();
			LivingForceManager.clearAll();
			PowerAbilityRuntime.onServerStopped(server);
			SpellCastingManager.clearAll();
			SpellFieldManager.clearAll();
			CelestialRuinManager.clearAll();
			RealmMindscapeManager.clearAll();
			ArtifactInventoryRuntime.clear();
			com.powers.fx.PowerFx.clearBudgets();
			PowersPackets.clearSyncCache();
			MagicFxPackets.clear();
			TravelChunkLoader.clear();
		});

		// passives get re-applied on a schedule so they never expire, toggles
		// re-assert themselves every few ticks (flight, forcefields), and time
		// stops, storms, and delayed jobs all advance each tick
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			int tick = server.getTickCount();
			MagicRuntime.global().tick(tick);
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
			PowerAbilityRuntime.tickTeleportMarking();
			ServerMagicScheduler.tick(tick);
		});

		LOGGER.info("POWERS framework initialized with {} power(s)", PowerRegistry.getAll().size());
	}

	/** Performs all work for one player during the coordinator's single pass. */
	static void tickPlayer(ServerPlayer player, int tick, PlayerTickCadence cadence) {
		if (cadence.passiveRefresh()) {
			refreshPassives(player);
			PowersPackets.syncTo(player);
		}
		ArtifactInventoryRuntime.tickPlayer(player, tick);
		enforceRealmGamemode(player);
		tickToggles(player, tick);
		PlayerPowers.PlayerPowersData data = PlayerPowers.get(player);
		boolean sleeping = player.isSleeping();
		boolean wasSleeping = WAS_SLEEPING.getOrDefault(player.getUUID(), false);
		WAS_SLEEPING.put(player.getUUID(), sleeping);
		if (cadence.second()) {
			SkillSystem.syncPathVisibility(player);
			SkillSystem.refresh(player);
		}
		if (wasSleeping && !sleeping) {
			data.restoreEnergy();
			PowersPackets.syncTo(player);
		} else if (cadence.second()) {
			int regen = 1;
			if (SkillSystem.hasDarknessTag(player)) {
				boolean inDarkRealm = SkillSystem.isDarkRealm(player.level().dimension());
				long timeOfDay = Math.floorMod(player.level().getDefaultClockTime(), 24000L);
				boolean night = timeOfDay >= 13000L || timeOfDay < 2300L;
				regen = PowerEnergy.darknessRegen(inDarkRealm || night);
			}
			if (data.regenerateEnergy(PowerScalingService.regeneration(player, regen))) {
				PowersPackets.syncTo(player);
			}
		}
		if (cadence.fiveTick()) {
			if (cadence.second()) AmethystDampening.update(player);
			drainExhaustionEnergy(player);
			tickAuras(player, tick);
		}
		if (cadence.second()) drainToggleEnergy(player);
	}

	/** Starts a visual lightning storm at a spot, lasting {@code ticks} ticks. */
	public static void startStorm(ServerLevel level, Vec3 position, int ticks) {
		startStorm(level, position, null, ticks, 0, StormTheme.NONE);
	}

	/** A storm at a spot that builds up the given realm's signature particles. */
	public static void startStorm(ServerLevel level, Vec3 position, int ticks, StormTheme theme) {
		startStorm(level, position, null, ticks, 0, theme);
	}

	/**
	 * Starts a storm that chases the given player, or only follows during
	 * the first {@code followTicks} ticks.
	 */
	public static void startStorm(ServerLevel level, Vec3 position, ServerPlayer follow, int ticks, int followTicks) {
		startStorm(level, position, follow, ticks, followTicks, StormTheme.NONE);
	}

	/**
	 * A storm that also echoes the realm its traveler is bound for, so the
	 * lightning beneath them builds up that realm's signature particles.
	 */
	public static void startStorm(ServerLevel level, Vec3 position, ServerPlayer follow, int ticks, int followTicks,
			StormTheme theme) {
		ServerMagicScheduler.startStorm(level, position, follow, ticks, followTicks, theme);
	}

	/** Runs {@code action} once, {@code ticks} server ticks from now. */
	public static ScheduledTaskQueue.TaskToken scheduleDelayed(
			MinecraftServer server, int ticks, Runnable action) {
		return ServerMagicScheduler.schedule(server, ticks, action);
	}

	// re-applies each power's passive effects with a long duration so they never lapse
	private static void refreshPassives(ServerPlayer player) {
		PlayerPowers.PlayerPowersData data = PlayerPowers.get(player);
		for (int slot = 0; slot < PlayerPowers.SLOT_COUNT; slot++) {
			Power power = data.getPower(slot);
			if (power == null) {
				continue;
			}
			for (PassiveEffect passive : power.passives()) {
				player.addEffect(PowerStatusEffects.hidden(passive.effect(), PASSIVE_REFRESH_TICKS * 3,
						passive.amplifier(), true, true));
			}
		}
	}

	/**
	 * Realm dimensions pin players to adventure so the scenery survives, and
	 * the old game mode comes back on the way out. The snapshot lives on the
	 * player as a persistent attachment rather than in a map: a player who logs
	 * out inside a realm used to come back, get adventure recorded as their
	 * "previous" mode, and stay stuck in it for good.
	 */
	private static void enforceRealmGamemode(ServerPlayer player) {
		if (PowerAbilityRuntime.usesDetachedBody(player.getUUID())) return;
		String dim = player.level().dimension().identifier().getPath();
		boolean inRealm = dim.equals("dark_realm") || dim.equals("light_realm") || dim.equals("middleworld");
		PlayerPowers.PlayerPowersData data = PlayerPowers.get(player);
		GameType previous = data.previousGameMode();
		if (inRealm) {
			// never snapshot adventure itself, or a relog inside the realm would
			// overwrite the real mode with the one we forced on them
			if (previous == null && player.gameMode() != GameType.ADVENTURE) {
				data.setPreviousGameMode(player.gameMode());
			}
			if (player.gameMode() != GameType.ADVENTURE) {
				player.setGameMode(GameType.ADVENTURE);
			}
		} else if (previous != null) {
			data.setPreviousGameMode(null);
			if (player.gameMode() == GameType.ADVENTURE) {
				player.setGameMode(previous);
			}
		}
	}

	// steps the per-tick effect of every toggle the player has switched on
	private static void tickToggles(ServerPlayer player, int serverTick) {
		PlayerPowers.PlayerPowersData data = PlayerPowers.get(player);
		for (int slot = 0; slot < PlayerPowers.SLOT_COUNT; slot++) {
			Power power = data.getPower(slot);
			if (power == null) {
				continue;
			}
			Ability ability = power.ability();
			if (ability != null && ability.isToggle() && data.isToggleActive(power.id().toString())
					&& serverTick % Math.max(1, ability.activeTickInterval()) == 0) {
				ability.tickActive(player, data);
			}
		}
	}

	// toggles that can't be paid shut themselves off, and burning out triggers the backlash
	private static void drainToggleEnergy(ServerPlayer player) {
		PlayerPowers.PlayerPowersData data = PlayerPowers.get(player);
		boolean anyDrainedOut = false;
		for (int slot = 0; slot < PlayerPowers.SLOT_COUNT; slot++) {
			Power power = data.getPower(slot);
			if (power == null || power.ability() == null || !power.ability().isToggle()
					|| !data.isToggleActive(power.id().toString())) continue;
			int cost = com.powers.power.PowerEnergy.ongoingCost(player, power.ability());
			if (cost > 0 && !data.consumeEnergy(cost)) {
				power.ability().activateToggleOff(player, data);
				data.setToggleActive(player, power.id().toString(), false);
				anyDrainedOut = true;
			}
		}
		if (anyDrainedOut) {
			energyBacklash(player);
			PowersPackets.syncTo(player);
		}
	}

	// the exhaustion effect eats the pool like hunger: every 5 ticks a chunk
	// is stripped away, bigger at higher amplifier, so the HUD visibly crashes
	// over a few seconds instead of zeroing out instantly
	private static void drainExhaustionEnergy(ServerPlayer player) {
		MobEffectInstance exhaustion = player.getEffect(PowersEffects.EXHAUSTION);
		if (exhaustion == null) return;
		PlayerPowers.PlayerPowersData data = PlayerPowers.get(player);
		int capacity = data.energyCapacity();
		int drain = Math.max(1, capacity / 20) * (1 + exhaustion.getAmplifier());
		int before = data.energy();
		data.drainEnergy(drain);
		if (data.energy() != before) {
			PowersPackets.syncTo(player);
		}
	}

	// letting a toggle burn out on an empty pool draws divine punishment:
	// 70% of max health in magic damage, the full godly wrath sequence, and a
	// lightning storm that chases the player, as if the gods themselves noticed
	private static void energyBacklash(ServerPlayer player) {
		ServerLevel level = (ServerLevel) player.level();

		float damage = player.getMaxHealth() * 0.7f;
		if (player.isAlive()) {
			player.hurtServer(level, player.damageSources().magic(), damage);
		}

		GodlyPunishment.strike(level, player, 0xFFD700, true);
		PowerMessages.sendImportant(player, "energy.powers.backlash", 6);
	}

	// drifting colored motes around the player, one hue per assigned power
	private static void tickAuras(ServerPlayer player, int tick) {
		ServerLevel level = (ServerLevel) player.level();
		PlayerPowers.PlayerPowersData data = PlayerPowers.get(player);
		for (int slot = 0; slot < PlayerPowers.SLOT_COUNT; slot++) {
			Power power = data.getPower(slot);
			if (power == null) {
				continue;
			}
			// flight cycles through the rainbow; every other power glows its own color
			int rgb = power.id().getPath().equals("flight")
					? com.powers.fx.PowerFx.rainbow(tick, 6)
					: power.color() & 0xFFFFFF;
			Vec3 pos = player.getEyePosition().add(
					(level.getRandom().nextDouble() - 0.5) * 0.8,
					(level.getRandom().nextDouble() - 0.5) * 0.8,
					(level.getRandom().nextDouble() - 0.5) * 0.8);
			com.powers.fx.PowerFx.coloredBurst(level, pos, rgb, 1, 0.02);
		}
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
