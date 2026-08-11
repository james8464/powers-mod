package com.powers.power.state;

import com.powers.PowersMod;
import com.powers.fx.TimeStopFx;
import com.powers.player.PlayerPowers;
import com.powers.power.AmethystDampening;
import com.powers.power.ToggleKeyRules;
import com.powers.util.PowerMessages;
import com.powers.companion.PrivateCompanionManager;
import com.powers.companion.ShadowCompanionEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.IdentityHashMap;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Optional;

/**
 * Owns the innate Time Stop's real server-wide {@code /tick freeze} state.
 *
 * <p>Only one player may own a server clock. Administrative freezes are never
 * stolen, every lifecycle exit releases an owned clock, and non-owners are
 * denied damage and magic while the vanilla tick manager suspends entities,
 * projectiles, block entities and scheduled ticks in every dimension.</p>
 */
public final class GlobalTimeStopManager {
	private enum Source { INNATE, CRYSTAL, SHADOW }

	private static final net.minecraft.resources.Identifier POWER_ID = PowersMod.id("time_freeze");
	private static final Map<MinecraftServer, Stop> ACTIVE = new IdentityHashMap<>();
	private static final Set<MinecraftServer> INTERNAL_CLOCK_WRITES =
			Collections.newSetFromMap(new IdentityHashMap<>());

	private GlobalTimeStopManager() {
	}

	/** Attempts to claim and freeze the global server clock for this player. */
	public static boolean start(ServerPlayer owner) {
		return start(owner, Source.INNATE, Long.MAX_VALUE);
	}

	/** Claims the same global clock for a fixed-duration crystal rite. */
	public static boolean startCrystal(ServerPlayer owner, int durationTicks) {
		long deadline = owner.level().getServer().getTickCount()
				+ Math.clamp(durationTicks, 1, 1_200);
		return start(owner, Source.CRYSTAL, deadline);
	}

	private static boolean start(ServerPlayer owner, Source source, long deadline) {
		MinecraftServer server = owner.level().getServer();
		if (!GlobalTimeStopRules.mayStart(ACTIVE.containsKey(server),
				server.tickRateManager().isFrozen())) {
			PowerMessages.overlay(owner, Component.translatable(
					"ability.powers.time_freeze.clock_owned"));
			return false;
		}
		Stop stop = new Stop(owner.getUUID(), source, deadline, null);
		ACTIVE.put(server, stop);
		if (!persist(server, stop)) {
			ACTIVE.remove(server);
			PowerMessages.overlay(owner, Component.literal(
					"Time refused to stop because its ownership journal could not be saved."));
			return false;
		}
		setFrozenOwned(server, true);
		for (ServerPlayer observer : server.getPlayerList().getPlayers()) {
			PowerMessages.overlay(observer, Component.translatable(
					"ability.powers.time_freeze.global_begin", owner.getDisplayName()));
			TimeStopFx.globalBegin((ServerLevel) observer.level(), observer.position(),
					stop.source == Source.CRYSTAL);
		}
		return true;
	}

	/** A manifested Shadow freezes the clock for its owner and pays from its own pool. */
	public static boolean startShadow(ServerPlayer owner, ShadowCompanionEntity shadow) {
		MinecraftServer server = owner.level().getServer();
		if (shadow == null || !shadow.isAlive() || shadow.ownerId() == null
				|| !shadow.ownerId().equals(owner.getUUID())
				|| !GlobalTimeStopRules.mayStart(ACTIVE.containsKey(server),
				server.tickRateManager().isFrozen())) return false;
		Stop stop = new Stop(owner.getUUID(), Source.SHADOW, Long.MAX_VALUE, shadow.getUUID());
		ACTIVE.put(server, stop);
		if (!persist(server, stop)) {
			ACTIVE.remove(server);
			return false;
		}
		setFrozenOwned(server, true);
		for (ServerPlayer observer : server.getPlayerList().getPlayers()) {
			PowerMessages.overlay(observer, Component.translatable(
					"ability.powers.time_freeze.global_begin", owner.getDisplayName()));
			TimeStopFx.globalBegin((ServerLevel) observer.level(), observer.position(), false);
		}
		return true;
	}

	/** Releases time only when the requester owns this server's clock. */
	public static void stop(ServerPlayer owner) {
		release(owner.level().getServer(), owner.getUUID(), true);
	}

	public static void stopShadow(ServerPlayer owner) {
		Stop stop = ACTIVE.get(owner.level().getServer());
		if (stop != null && stop.source == Source.SHADOW) {
			release(owner.level().getServer(), owner.getUUID(), true);
		}
	}

	/** Releases only a crystal-owned stop; innate toggles retain their own authority. */
	public static void stopCrystal(ServerPlayer owner) {
		Stop stop = ACTIVE.get(owner.level().getServer());
		if (stop != null && stop.source == Source.CRYSTAL) {
			release(owner.level().getServer(), owner.getUUID(), true);
		}
	}

	public static boolean isCrystalOwnedBy(ServerPlayer player) {
		if (player == null) return false;
		Stop stop = ACTIVE.get(player.level().getServer());
		return stop != null && stop.source == Source.CRYSTAL
				&& stop.owner().equals(player.getUUID());
	}

	/** Read-only HUD snapshot of the same owner/deadline that controls the true server clock. */
	public static Optional<ClockSnapshot> snapshot(MinecraftServer server) {
		Stop stop = ACTIVE.get(server);
		if (stop == null) return Optional.empty();
		long remaining = stop.deadline == Long.MAX_VALUE ? -1L
				: GlobalTimeStopRules.remainingTicks(server.getTickCount(), stop.deadline);
		return Optional.of(new ClockSnapshot(stop.owner, stop.source.name(), stop.deadline, remaining));
	}

	public record ClockSnapshot(UUID owner, String source, long deadline, long remainingTicks) { }

	/** Advances lifecycle checks and sparse cross-dimensional clock visuals. */
	public static void tick(MinecraftServer server) {
		Stop stop = ACTIVE.get(server);
		if (stop == null) return;
		ServerPlayer owner = server.getPlayerList().getPlayer(stop.owner());
		boolean online = owner != null;
		boolean alive = online && owner.isAlive();
		ShadowCompanionEntity shadow = stop.source == Source.SHADOW && online
				? PrivateCompanionManager.body(stop.owner()).orElse(null) : null;
		boolean authorityActive = online && switch (stop.source) {
			case CRYSTAL -> server.getTickCount() < stop.deadline;
			case INNATE -> ToggleKeyRules.anyOwnsAbility(
					PlayerPowers.get(owner).getActiveToggles(), POWER_ID);
			case SHADOW -> shadow != null && shadow.isAlive()
					&& shadow.getUUID().equals(stop.shadowBody)
					&& shadow.energy() >= 300;
		};
		boolean dampened = online && (stop.source == Source.SHADOW
				? shadow == null || AmethystDampening.isDampened(shadow)
				: AmethystDampening.isDampened(owner));
		if (GlobalTimeStopRules.shouldRelease(online, alive, authorityActive, dampened,
				server.tickRateManager().isFrozen(), stop.externallyMutated)) {
			release(server, stop.owner(), true);
			return;
		}
		if (stop.source == Source.SHADOW && server.getTickCount() % 20 == 0) {
			shadow.setEnergy(shadow.energy() - 300);
		}
		if (server.getTickCount() % 20 == 0) {
			for (ServerPlayer observer : server.getPlayerList().getPlayers()) {
				TimeStopFx.globalSustain((ServerLevel) observer.level(), observer.position(),
						server.getTickCount(), stop.source == Source.CRYSTAL);
				if (stop.source == Source.CRYSTAL) {
					observer.sendSystemMessage(Component.literal("Temporal fracture — owner "
							+ owner.getScoreboardName() + ", release in "
							+ GlobalTimeStopRules.remainingTicks(server.getTickCount(), stop.deadline)
							+ " ticks"), true);
				}
			}
		}
	}

	/** Returns true when the actor is allowed to act under the current clock owner. */
	public static boolean mayAct(ServerPlayer actor) {
		Stop stop = ACTIVE.get(actor.level().getServer());
		return GlobalTimeStopRules.mayAct(stop == null ? null : stop.owner(), actor.getUUID());
	}

	/** Rejects a non-owner action with concise feedback. */
	public static boolean rejectIfStopped(ServerPlayer actor) {
		if (mayAct(actor)) return false;
		PowerMessages.overlay(actor, Component.translatable(
				"ability.powers.time_freeze.frozen"));
		return true;
	}

	/** True while this server has a player-owned clock freeze. */
	public static boolean isStopped(MinecraftServer server) {
		return ACTIVE.containsKey(server);
	}

	/** Clears one departing/respawning owner and restores the server clock. */
	public static void clear(MinecraftServer server, UUID owner) {
		release(server, owner, true);
	}

	/** Restores the clock before server-owned runtime state is discarded. */
	public static void clearAll(MinecraftServer server) {
		Stop stop = ACTIVE.get(server);
		if (stop != null) release(server, stop.owner(), false);
	}

	/** Clears a crash journal before players enter play and never steals an unjournalled admin freeze. */
	public static void reconcileStartup(MinecraftServer server) {
		TimeStopSavedData data = savedData(server);
		if (!data.snapshot().active()) return;
		ACTIVE.remove(server);
		if (server.tickRateManager().isFrozen()) setFrozenOwned(server, false);
		data.clear();
		server.overworld().getDataStorage().saveAndJoin();
		PowersMod.LOGGER.warn("Recovered a stale POWERS Time Stop ownership journal from a prior shutdown");
	}

	/** Called by the tick-manager mixin whenever code outside this manager writes freeze state. */
	public static void observeClockWrite(MinecraftServer server) {
		Stop stop = ACTIVE.get(server);
		if (stop != null && !INTERNAL_CLOCK_WRITES.contains(server)) {
			stop.externallyMutated = true;
		}
	}

	private static void release(MinecraftServer server, UUID expectedOwner, boolean announce) {
		Stop stop = ACTIVE.get(server);
		if (stop == null || !stop.owner().equals(expectedOwner)) return;
		ACTIVE.remove(server);
		if (GlobalTimeStopRules.shouldUnfreezeOnRelease(
				server.tickRateManager().isFrozen(), stop.externallyMutated)) {
			setFrozenOwned(server, false);
		}
		clearPersisted(server);
		ServerPlayer owner = server.getPlayerList().getPlayer(expectedOwner);
		if (owner != null && stop.source == Source.INNATE) {
			PlayerPowers.PlayerPowersData data = PlayerPowers.get(owner);
			for (String toggleKey : java.util.List.copyOf(data.getActiveToggles())) {
				if (ToggleKeyRules.ownsAbility(toggleKey, POWER_ID)) {
					data.setToggleActive(owner, toggleKey, false);
				}
			}
		}
		if (!announce) return;
		for (ServerPlayer observer : server.getPlayerList().getPlayers()) {
			PowerMessages.overlay(observer, Component.translatable(
					"ability.powers.time_freeze.global_release"));
			TimeStopFx.globalRelease((ServerLevel) observer.level(), observer.position(),
					stop.source == Source.CRYSTAL);
		}
	}

	private static void setFrozenOwned(MinecraftServer server, boolean frozen) {
		INTERNAL_CLOCK_WRITES.add(server);
		try {
			server.tickRateManager().setFrozen(frozen);
		} finally {
			INTERNAL_CLOCK_WRITES.remove(server);
		}
	}

	private static boolean persist(MinecraftServer server, Stop stop) {
		try {
			TimeStopSavedData data = savedData(server);
			data.activate(stop.owner().toString(), stop.source.name(), stop.deadline,
					stop.shadowBody == null ? "" : stop.shadowBody.toString());
			server.overworld().getDataStorage().saveAndJoin();
			return true;
		} catch (RuntimeException failure) {
			PowersMod.LOGGER.error("Could not persist Time Stop ownership", failure);
			return false;
		}
	}

	private static void clearPersisted(MinecraftServer server) {
		try {
			TimeStopSavedData data = savedData(server);
			data.clear();
			server.overworld().getDataStorage().saveAndJoin();
		} catch (RuntimeException failure) {
			PowersMod.LOGGER.error("Could not clear Time Stop ownership journal", failure);
		}
	}

	private static TimeStopSavedData savedData(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(TimeStopSavedData.TYPE);
	}

	private static final class Stop {
		private final UUID owner;
		private final Source source;
		private final long deadline;
		private final UUID shadowBody;
		private boolean externallyMutated;

		private Stop(UUID owner, Source source, long deadline, UUID shadowBody) {
			this.owner = owner;
			this.source = source;
			this.deadline = deadline;
			this.shadowBody = shadowBody;
		}

		private UUID owner() {
			return owner;
		}
	}
}
