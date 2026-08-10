package com.powers.power.travel;

import com.powers.PowersMod;
import com.powers.util.LoadedChunks;
import com.powers.util.ScheduledTaskQueue;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/** Requests one destination per player with bounded readiness and exact ticket cleanup. */
public final class TravelChunkLoader {
	public static final int MAX_FOLLOWUP_TICKS = 60;
	public static final Budget DEFAULT_BUDGET = new Budget(80, 200);
	private static final int TICKET_RADIUS = 1;
	private static final Map<UUID, Pending> PENDING = new HashMap<>();
	private static final Map<UUID, Lease> READY_LEASES = new HashMap<>();

	/* Keep Minecraft's bootstrapped TicketType lazy so pure policy tests can load this utility safely. */
	private static final class TicketHolder {
		private static final TicketType TRAVEL = new TicketType(
				DEFAULT_BUDGET.ticketTicks(), TicketType.FLAG_LOADING);
	}

	public enum Resolution { READY, TIMEOUT, REPLACED, CANCELLED }

	/** Pure exact-once state shared by future, timeout, replacement, and disconnect paths. */
	public static final class RequestState {
		private final AtomicReference<Resolution> resolution = new AtomicReference<>();

		public boolean resolve(Resolution outcome) {
			return resolution.compareAndSet(null, Objects.requireNonNull(outcome, "outcome"));
		}

		public Resolution resolution() {
			return resolution.get();
		}
	}

	/** Server-tick bounds for one load request and its later teleport animation. */
	public record Budget(int waitTicks, int ticketTicks) {
		public Budget {
			if (waitTicks < 1 || ticketTicks < waitTicks + MAX_FOLLOWUP_TICKS) {
				throw new IllegalArgumentException("Travel tickets must cover loading and follow-up travel");
			}
		}

		public boolean readyInTime(long startedAt, long readyAt) {
			return readyAt >= startedAt && readyAt - startedAt <= waitTicks;
		}
	}

	private static final class Pending {
		private final UUID owner;
		private final ServerLevel level;
		private final BlockPos destination;
		private final ChunkPos chunk;
		private final long startedAt;
		private final Runnable ready;
		private final Runnable failed;
		private final RequestState state = new RequestState();
		private ScheduledTaskQueue.TaskToken timeoutToken;

		private Pending(UUID owner, ServerLevel level, BlockPos destination,
				long startedAt, Runnable ready, Runnable failed) {
			this.owner = owner;
			this.level = level;
			this.destination = destination.immutable();
			this.chunk = ChunkPos.containing(destination);
			this.startedAt = startedAt;
			this.ready = ready;
			this.failed = failed;
		}
	}

	private record Lease(UUID owner, ServerLevel level, ChunkPos chunk,
			ScheduledTaskQueue.TaskToken releaseToken) {
	}

	private TravelChunkLoader() {
	}

	/** Starts or replaces the sole pending destination load owned by a player UUID. */
	public static boolean request(UUID owner, ServerLevel level, BlockPos destination,
			Runnable ready, Runnable timedOut) {
		Objects.requireNonNull(owner, "owner");
		Objects.requireNonNull(level, "level");
		Objects.requireNonNull(destination, "destination");
		Objects.requireNonNull(ready, "ready");
		Objects.requireNonNull(timedOut, "timedOut");
		MinecraftServer server = level.getServer();
		if (server == null) return false;
		releaseLease(owner);
		Pending previous = PENDING.get(owner);
		if (previous != null) settle(previous, Resolution.REPLACED);
		Pending pending = new Pending(owner, level, destination, server.getTickCount(), ready, timedOut);
		PENDING.put(owner, pending);
		pending.timeoutToken = PowersMod.scheduleDelayed(server, DEFAULT_BUDGET.waitTicks() + 1,
				() -> settle(pending, Resolution.TIMEOUT));
		if (!pending.timeoutToken.accepted()) {
			settle(pending, Resolution.TIMEOUT);
			return false;
		}
		try {
			level.getChunkSource().addTicketAndLoadWithRadius(TicketHolder.TRAVEL, pending.chunk, TICKET_RADIUS)
					.whenComplete((ignored, error) -> server.execute(() -> {
						boolean loaded = error == null && DEFAULT_BUDGET.readyInTime(
								pending.startedAt, server.getTickCount())
								&& LoadedChunks.contains(level, pending.destination);
						settle(pending, loaded ? Resolution.READY : Resolution.TIMEOUT);
					}));
		} catch (RuntimeException error) {
			settle(pending, Resolution.TIMEOUT);
			return false;
		}
		return true;
	}

	/** Cancels one player's pending load on disconnect or lifecycle invalidation. */
	public static boolean cancel(UUID owner) {
		Pending pending = PENDING.get(owner);
		return pending != null && settle(pending, Resolution.CANCELLED);
	}

	/** Releases all live tickets at server shutdown. */
	public static void clear() {
		for (Pending pending : new ArrayList<>(PENDING.values())) {
			settle(pending, Resolution.CANCELLED);
		}
		PENDING.clear();
		for (UUID owner : new ArrayList<>(READY_LEASES.keySet())) releaseLease(owner);
		READY_LEASES.clear();
	}

	/** Number of bounded, temporary destination-loading tickets. */
	public static int pendingRequestCount() {
		return PENDING.size() + READY_LEASES.size();
	}

	/** Ready destinations retain their ticket while delayed storms and body creation finish. */
	static int releaseDelayTicks(Resolution resolution) {
		return resolution == Resolution.READY ? MAX_FOLLOWUP_TICKS : 0;
	}

	private static boolean settle(Pending pending, Resolution resolution) {
		if (!pending.state.resolve(resolution)) return false;
		PENDING.remove(pending.owner, pending);
		if (pending.timeoutToken != null) pending.timeoutToken.cancel();
		if (resolution == Resolution.READY) {
			int delay = releaseDelayTicks(resolution);
			ScheduledTaskQueue.TaskToken token = PowersMod.scheduleDelayed(
					pending.level.getServer(), delay, () -> releaseLease(pending.owner));
			if (token.accepted()) {
				READY_LEASES.put(pending.owner,
						new Lease(pending.owner, pending.level, pending.chunk, token));
			} else {
				removeTicket(pending.level, pending.chunk);
			}
			pending.ready.run();
		} else {
			removeTicket(pending.level, pending.chunk);
			pending.failed.run();
		}
		return true;
	}

	private static void releaseLease(UUID owner) {
		Lease lease = READY_LEASES.remove(owner);
		if (lease == null) return;
		lease.releaseToken().cancel();
		removeTicket(lease.level(), lease.chunk());
	}

	private static void removeTicket(ServerLevel level, ChunkPos chunk) {
		level.getChunkSource().removeTicketWithRadius(TicketHolder.TRAVEL, chunk, TICKET_RADIUS);
	}
}
