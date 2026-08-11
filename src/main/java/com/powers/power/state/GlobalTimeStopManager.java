package com.powers.power.state;

import com.powers.PowersMod;
import com.powers.fx.TimeStopFx;
import com.powers.player.PlayerPowers;
import com.powers.power.AmethystDampening;
import com.powers.power.ToggleKeyRules;
import com.powers.util.PowerMessages;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.IdentityHashMap;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Owns the innate Time Stop's real server-wide {@code /tick freeze} state.
 *
 * <p>Only one player may own a server clock. Administrative freezes are never
 * stolen, every lifecycle exit releases an owned clock, and non-owners are
 * denied damage and magic while the vanilla tick manager suspends entities,
 * projectiles, block entities and scheduled ticks in every dimension.</p>
 */
public final class GlobalTimeStopManager {
	private enum Source { INNATE, CRYSTAL }

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
		Stop stop = new Stop(owner.getUUID(), source, deadline);
		ACTIVE.put(server, stop);
		setFrozenOwned(server, true);
		for (ServerPlayer observer : server.getPlayerList().getPlayers()) {
			PowerMessages.overlay(observer, Component.translatable(
					"ability.powers.time_freeze.global_begin", owner.getDisplayName()));
			TimeStopFx.globalBegin((ServerLevel) observer.level(), observer.position(),
					stop.source == Source.CRYSTAL);
		}
		return true;
	}

	/** Releases time only when the requester owns this server's clock. */
	public static void stop(ServerPlayer owner) {
		release(owner.level().getServer(), owner.getUUID(), true);
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

	/** Advances lifecycle checks and sparse cross-dimensional clock visuals. */
	public static void tick(MinecraftServer server) {
		Stop stop = ACTIVE.get(server);
		if (stop == null) return;
		ServerPlayer owner = server.getPlayerList().getPlayer(stop.owner());
		boolean online = owner != null;
		boolean alive = online && owner.isAlive();
		boolean authorityActive = online && (stop.source == Source.CRYSTAL
				? server.getTickCount() < stop.deadline
				: ToggleKeyRules.anyOwnsAbility(
						PlayerPowers.get(owner).getActiveToggles(), POWER_ID));
		boolean dampened = online && AmethystDampening.isDampened(owner);
		if (GlobalTimeStopRules.shouldRelease(online, alive, authorityActive, dampened,
				server.tickRateManager().isFrozen(), stop.externallyMutated)) {
			release(server, stop.owner(), true);
			return;
		}
		if (server.getTickCount() % 20 == 0) {
			for (ServerPlayer observer : server.getPlayerList().getPlayers()) {
				TimeStopFx.globalSustain((ServerLevel) observer.level(), observer.position(),
						server.getTickCount(), stop.source == Source.CRYSTAL);
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

	private static final class Stop {
		private final UUID owner;
		private final Source source;
		private final long deadline;
		private boolean externallyMutated;

		private Stop(UUID owner, Source source, long deadline) {
			this.owner = owner;
			this.source = source;
			this.deadline = deadline;
		}

		private UUID owner() {
			return owner;
		}
	}
}
