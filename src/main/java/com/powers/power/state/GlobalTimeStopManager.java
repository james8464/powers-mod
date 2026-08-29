package com.powers.power.state;

import com.powers.PowersMod;
import com.powers.fx.TimeStopFx;
import com.powers.player.PlayerPowers;
import com.powers.power.AmethystDampening;
import com.powers.power.ToggleKeyRules;
import com.powers.util.PowerMessages;
import com.powers.companion.PrivateCompanionManager;
import com.powers.companion.ShadowCompanionEntity;
import com.powers.time.ControlTick;
import com.powers.time.TemporalClocks;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.IdentityHashMap;
import java.util.Map;
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
	private static final net.minecraft.resources.Identifier POWER_ID = PowersMod.id("time_freeze");
	private static final Map<MinecraftServer, TimeStopLeaseBook> BOOKS = new IdentityHashMap<>();
	private static final Map<MinecraftServer, Long> INTERNAL_CLOCK_WRITES = new IdentityHashMap<>();

	private GlobalTimeStopManager() {
	}

	/** Attempts to claim and freeze the global server clock for this player. */
	public static boolean start(ServerPlayer owner) {
		return start(owner, TimeStopLeaseSource.INNATE, Long.MAX_VALUE, null);
	}

	/** Claims the same global clock for a fixed-duration crystal rite. */
	public static boolean startCrystal(ServerPlayer owner, int durationTicks) {
		return start(owner, TimeStopLeaseSource.CRYSTAL,
				Math.clamp(durationTicks, 1, 1_200), null);
	}

	private static boolean start(ServerPlayer owner, TimeStopLeaseSource source,
			long durationTicks, UUID shadowBody) {
		MinecraftServer server = owner.level().getServer();
		Optional<TimeStopLease> acquired = book(server).acquire(owner.getUUID(), source,
				TemporalClocks.control(server), durationTicks, shadowBody,
				server.tickRateManager().isFrozen());
		if (acquired.isEmpty()) {
			PowerMessages.overlay(owner, Component.translatable(
					"ability.powers.time_freeze.clock_owned"));
			return false;
		}
		TimeStopLease lease = acquired.orElseThrow();
		if (!persist(server, lease)) {
			book(server).release(lease.token(), lease.owner(), lease.source(), false);
			PowerMessages.overlay(owner, Component.literal(
					"Time refused to stop because its ownership journal could not be saved."));
			return false;
		}
		try {
			setFrozenOwned(server, true, lease.token());
		} catch (RuntimeException failure) {
			book(server).release(lease.token(), lease.owner(), lease.source(), false);
			clearPersisted(server);
			PowersMod.LOGGER.error("Could not acquire the vanilla frozen clock", failure);
			return false;
		}
		for (ServerPlayer observer : server.getPlayerList().getPlayers()) {
			PowerMessages.overlay(observer, Component.translatable(
					"ability.powers.time_freeze.global_begin", owner.getDisplayName()));
			TimeStopFx.globalBegin((ServerLevel) observer.level(), observer.position(),
					lease.source() == TimeStopLeaseSource.CRYSTAL);
		}
		return true;
	}

	/** A manifested Shadow freezes the clock for its owner and pays from its own pool. */
	public static boolean startShadow(ServerPlayer owner, ShadowCompanionEntity shadow) {
		if (shadow == null || !shadow.isAlive() || shadow.ownerId() == null
				|| !shadow.ownerId().equals(owner.getUUID())) return false;
		return start(owner, TimeStopLeaseSource.SHADOW, Long.MAX_VALUE, shadow.getUUID());
	}

	/** Releases time only when the requester owns this server's clock. */
	public static void stop(ServerPlayer owner) {
		releaseCurrent(owner.level().getServer(), owner.getUUID(),
				TimeStopLeaseSource.INNATE, true);
	}

	public static void stopShadow(ServerPlayer owner) {
		releaseCurrent(owner.level().getServer(), owner.getUUID(),
				TimeStopLeaseSource.SHADOW, true);
	}

	/** Releases only a crystal-owned stop; innate toggles retain their own authority. */
	public static void stopCrystal(ServerPlayer owner) {
		releaseCurrent(owner.level().getServer(), owner.getUUID(),
				TimeStopLeaseSource.CRYSTAL, true);
	}

	public static boolean isCrystalOwnedBy(ServerPlayer player) {
		if (player == null) return false;
		TimeStopLease lease = active(player.level().getServer());
		return lease != null && lease.source() == TimeStopLeaseSource.CRYSTAL
				&& lease.owner().equals(player.getUUID());
	}

	/** Read-only HUD snapshot of the same owner/deadline that controls the true server clock. */
	public static Optional<ClockSnapshot> snapshot(MinecraftServer server) {
		TimeStopLease lease = active(server);
		if (lease == null) return Optional.empty();
		ControlTick now = TemporalClocks.control(server);
		long remaining = lease.indefinite() ? -1L : now.remainingUntil(lease.deadline());
		return Optional.of(new ClockSnapshot(lease.owner(), lease.source().name(),
				lease.deadline().value(), remaining, lease.token(), "CONTROL"));
	}

	public record ClockSnapshot(UUID owner, String source, long deadline, long remainingTicks,
			long leaseToken, String clock) { }

	/** Advances lifecycle checks and sparse cross-dimensional clock visuals. */
	public static void tick(MinecraftServer server) {
		TimeStopLease lease = active(server);
		if (lease == null) return;
		ControlTick now = TemporalClocks.control(server);
		ServerPlayer owner = server.getPlayerList().getPlayer(lease.owner());
		boolean online = owner != null;
		boolean alive = online && owner.isAlive();
		ShadowCompanionEntity shadow = lease.source() == TimeStopLeaseSource.SHADOW && online
				? PrivateCompanionManager.body(lease.owner()).orElse(null) : null;
		boolean authorityActive = online && switch (lease.source()) {
			case CRYSTAL -> !TimeStopLeaseRules.expired(lease, now);
			case INNATE -> ToggleKeyRules.anyOwnsAbility(
					PlayerPowers.get(owner).getActiveToggles(), POWER_ID);
			case SHADOW -> shadow != null && shadow.isAlive()
					&& shadow.getUUID().equals(lease.shadowBody())
					&& shadow.energy() >= 300;
		};
		boolean dampened = online && (lease.source() == TimeStopLeaseSource.SHADOW
				? shadow == null || AmethystDampening.isDampened(shadow)
				: AmethystDampening.isDampened(owner));
		if (GlobalTimeStopRules.shouldRelease(online, alive, authorityActive, dampened,
				server.tickRateManager().isFrozen(), lease.externallySuperseded())) {
			releaseExact(server, lease, true);
			return;
		}
		if (lease.source() == TimeStopLeaseSource.SHADOW && now.value() % 20L == 0L) {
			shadow.setEnergy(shadow.energy() - 300);
		}
		if (now.value() % 20L == 0L) {
			for (ServerPlayer observer : server.getPlayerList().getPlayers()) {
				TimeStopFx.globalSustain((ServerLevel) observer.level(), observer.position(),
						now.value(), lease.source() == TimeStopLeaseSource.CRYSTAL);
				if (lease.source() == TimeStopLeaseSource.CRYSTAL) {
					long seconds = (now.remainingUntil(lease.deadline()) + 19L) / 20L;
					observer.sendSystemMessage(Component.translatable("crystal.powers.chrono_status",
							owner.getDisplayName(), seconds), true);
				}
			}
		}
	}

	/** Returns true when the actor is allowed to act under the current clock owner. */
	public static boolean mayAct(ServerPlayer actor) {
		TimeStopLease lease = active(actor.level().getServer());
		return GlobalTimeStopRules.mayAct(lease == null ? null : lease.owner(), actor.getUUID());
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
		return active(server) != null;
	}

	/** Clears one departing/respawning owner and restores the server clock. */
	public static void clear(MinecraftServer server, UUID owner) {
		TimeStopLease lease = active(server);
		if (lease != null && lease.owner().equals(owner)) releaseExact(server, lease, true);
	}

	/** Restores the clock before server-owned runtime state is discarded. */
	public static void clearAll(MinecraftServer server) {
		TimeStopLease lease = active(server);
		if (lease != null) releaseExact(server, lease, false);
		BOOKS.remove(server);
		INTERNAL_CLOCK_WRITES.remove(server);
	}

	/** Clears a crash journal before players enter play and never steals an unjournalled admin freeze. */
	public static void reconcileStartup(MinecraftServer server) {
		TimeStopSavedData.RecoveryDecision recovery;
		try {
			recovery = TimeStopJournalStore.forServer(server).read().recoveryDecision();
		} catch (java.io.IOException malformed) {
			recovery = TimeStopSavedData.RecoveryDecision.CLEAR_ONLY;
			PowersMod.LOGGER.error("Could not decode Time Stop ownership journal", malformed);
		}
		if (!recovery.clearJournal()) return;
		if (!clearPersisted(server)) {
			PowersMod.LOGGER.error("Could not retire Time Stop recovery authority; vanilla clock unchanged");
			return;
		}
		BOOKS.remove(server);
		if (recovery.unfreeze() && server.tickRateManager().isFrozen()) {
			server.tickRateManager().setFrozen(false);
		}
		if (recovery.unfreeze()) {
			PowersMod.LOGGER.warn("Recovered a validated stale POWERS Time Stop ownership journal");
		} else {
			PowersMod.LOGGER.warn("Cleared a malformed Time Stop journal without changing vanilla freeze state");
		}
	}

	/** Called by the tick-manager mixin whenever code outside this manager writes freeze state. */
	public static void observeClockWrite(MinecraftServer server) {
		TimeStopLease lease = active(server);
		if (lease == null) return;
		Long internalToken = INTERNAL_CLOCK_WRITES.get(server);
		if (internalToken == null || internalToken.longValue() != lease.token()) {
			if (!book(server).observeExternalWrite(() -> clearPersisted(server))) {
				throw new IllegalStateException(
						"External clock write refused because POWERS authority could not be retired");
			}
		}
	}

	private static void releaseCurrent(MinecraftServer server, UUID owner,
			TimeStopLeaseSource source, boolean announce) {
		TimeStopLease lease = active(server);
		if (lease != null && lease.owner().equals(owner) && lease.source() == source) {
			releaseExact(server, lease, announce);
		}
	}

	private static void releaseExact(MinecraftServer server, TimeStopLease lease, boolean announce) {
		TimeStopLease current = active(server);
		if (current == null || !current.equals(lease)) return;
		// Retire durable thaw authority before either process ownership or the
		// vanilla clock changes. A failed tombstone leaves the owned freeze intact.
		if (!clearPersisted(server)) return;
		TimeStopLeaseBook.ReleaseDecision decision = book(server).release(lease.token(),
				lease.owner(), lease.source(), server.tickRateManager().isFrozen());
		if (!decision.matched()) return;
		if (decision.unfreeze()) setFrozenOwned(server, false, lease.token());
		clearPersisted(server);
		ServerPlayer owner = server.getPlayerList().getPlayer(lease.owner());
		if (owner != null && lease.source() == TimeStopLeaseSource.INNATE) {
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
					lease.source() == TimeStopLeaseSource.CRYSTAL);
		}
	}

	private static void setFrozenOwned(MinecraftServer server, boolean frozen, long leaseToken) {
		INTERNAL_CLOCK_WRITES.put(server, leaseToken);
		try {
			server.tickRateManager().setFrozen(frozen);
		} finally {
			INTERNAL_CLOCK_WRITES.remove(server);
		}
	}

	private static boolean persist(MinecraftServer server, TimeStopLease lease) {
		TimeStopSavedData data = savedData(server);
		TimeStopSavedData.Snapshot previous = data.snapshot();
		data.activate(lease);
		try {
			TimeStopJournalStore.forServer(server).writeVerified(data.snapshot());
			data.replacePersisted(data.snapshot());
			return true;
		} catch (java.io.IOException | RuntimeException failure) {
			data.replacePersisted(previous);
			PowersMod.LOGGER.error("Could not persist Time Stop ownership", failure);
			return false;
		}
	}

	private static boolean clearPersisted(MinecraftServer server) {
		TimeStopSavedData data = savedData(server);
		TimeStopSavedData.Snapshot previous = data.snapshot();
		try {
			TimeStopJournalStore.forServer(server).writeVerified(
					TimeStopSavedData.emptySnapshot());
			data.replacePersisted(TimeStopSavedData.emptySnapshot());
			return true;
		} catch (java.io.IOException | RuntimeException failure) {
			data.replacePersisted(previous);
			PowersMod.LOGGER.error("Could not clear Time Stop ownership journal", failure);
			return false;
		}
	}

	private static TimeStopSavedData savedData(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(TimeStopSavedData.TYPE);
	}

	private static TimeStopLeaseBook book(MinecraftServer server) {
		return BOOKS.computeIfAbsent(server, ignored -> new TimeStopLeaseBook());
	}

	private static TimeStopLease active(MinecraftServer server) {
		TimeStopLeaseBook book = BOOKS.get(server);
		return book == null ? null : book.active().orElse(null);
	}
}
