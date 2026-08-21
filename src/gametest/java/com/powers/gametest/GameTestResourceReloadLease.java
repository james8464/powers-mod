package com.powers.gametest;

import net.minecraft.server.MinecraftServer;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/** Serializes the few GameTests that mutate or reload the process-wide resource manager. */
public final class GameTestResourceReloadLease {
	private static final Queue<Request> WAITERS = new ArrayDeque<>();
	private static boolean held;

	private GameTestResourceReloadLease() {
	}

	public static void acquire(MinecraftServer server, Consumer<Lease> action) {
		Request request = new Request(server, action);
		boolean start;
		synchronized (WAITERS) {
			start = !held;
			if (start) held = true;
			else WAITERS.add(request);
		}
		if (start) start(request);
	}

	private static void start(Request request) {
		request.server().execute(() -> {
			Lease lease = new Lease();
			try {
				request.action().accept(lease);
			} catch (Throwable failure) {
				lease.close();
				throw failure;
			}
		});
	}

	public static final class Lease implements AutoCloseable {
		private final AtomicBoolean closed = new AtomicBoolean();

		@Override
		public void close() {
			if (!closed.compareAndSet(false, true)) return;
			Request next;
			synchronized (WAITERS) {
				next = WAITERS.poll();
				if (next == null) held = false;
			}
			if (next != null) start(next);
		}
	}

	private record Request(MinecraftServer server, Consumer<Lease> action) {
	}
}
