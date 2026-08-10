package com.powers.power.travel;

import com.powers.PowersMod;
import com.powers.util.LoadedChunks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;

import java.util.concurrent.atomic.AtomicBoolean;

/** Requests a small destination area with tick-bounded readiness and self-expiring tickets. */
public final class TravelChunkLoader {
	public static final int MAX_FOLLOWUP_TICKS = 60;
	public static final Budget DEFAULT_BUDGET = new Budget(80, 200);
	private static final int TICKET_RADIUS = 1;

	/* Keep Minecraft's bootstrapped TicketType lazy so pure policy tests can load this utility safely. */
	private static final class TicketHolder {
		private static final TicketType TRAVEL = new TicketType(
				DEFAULT_BUDGET.ticketTicks(), TicketType.FLAG_LOADING);
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

	private TravelChunkLoader() {
	}

	/**
	 * Starts a server-owned load request. Once accepted, exactly one callback
	 * runs; the ticket expires naturally even if chunk generation never completes.
	 */
	public static boolean request(ServerLevel level, BlockPos destination,
			Runnable ready, Runnable timedOut) {
		MinecraftServer server = level.getServer();
		if (server == null) return false;
		long startedAt = server.getTickCount();
		AtomicBoolean resolved = new AtomicBoolean();
		ChunkPos chunk = ChunkPos.containing(destination);
		try {
			level.getChunkSource().addTicketAndLoadWithRadius(TicketHolder.TRAVEL, chunk, TICKET_RADIUS)
					.whenComplete((ignored, error) -> server.execute(() -> {
						if (!resolved.compareAndSet(false, true)) return;
						boolean loaded = error == null && DEFAULT_BUDGET.readyInTime(
								startedAt, server.getTickCount()) && LoadedChunks.contains(level, destination);
						if (loaded) ready.run();
						else timedOut.run();
					}));
		} catch (RuntimeException error) {
			return false;
		}
		PowersMod.scheduleDelayed(server, DEFAULT_BUDGET.waitTicks() + 1, () -> {
			if (resolved.compareAndSet(false, true)) timedOut.run();
		});
		return true;
	}
}
