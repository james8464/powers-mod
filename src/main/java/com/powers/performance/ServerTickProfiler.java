package com.powers.performance;

import com.powers.PowersMod;
import com.powers.diagnostics.ServerRuntimeMetrics;
import jdk.jfr.Recording;
import net.minecraft.server.MinecraftServer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.locks.LockSupport;

/**
 * Opt-in dedicated-server tick profiler. It records actual full-tick duration,
 * JFR data, connected-player counts, and POWERS work peaks; idle servers pay
 * only one map lookup at each tick boundary.
 */
public final class ServerTickProfiler {
	private static final long TARGET_TICK_NANOS = 50_000_000L;

	public record Status(boolean active, int expectedPlayers, int connectedPlayers,
			int sampledTicks, int requestedTicks, String label) { }

	private static final Map<MinecraftServer, Session> SESSIONS = new WeakHashMap<>();

	private static final class Session {
		private final int expectedPlayers;
		private final int requestedTicks;
		private final String label;
		private final List<Long> durations;
		private final Recording recording;
		private final boolean paceRealTime;
		private final long startedAt;
		private long logicalTick = Long.MIN_VALUE;
		private long sampledTick = Long.MIN_VALUE;
		private long tickStarted;
		private long attemptedActions;
		private long successfulActions;
		private int peakParticles;
		private int peakPackets;
		private int peakInspections;

		private Session(int expectedPlayers, int requestedTicks, String label,
				boolean paceRealTime) {
			this.expectedPlayers = expectedPlayers;
			this.requestedTicks = requestedTicks;
			this.label = label;
			this.paceRealTime = paceRealTime;
			durations = new ArrayList<>(requestedTicks);
			recording = createRecording(label);
			startedAt = System.nanoTime();
		}
	}

	private ServerTickProfiler() {
	}

	/** Starts one bounded profile; returns false when a session already owns the server. */
	public static boolean start(MinecraftServer server, int expectedPlayers,
			int requestedTicks, String label) {
		return start(server, expectedPlayers, requestedTicks, label, false);
	}

	/** Starts a profile and optionally paces a fast headless harness at 20 TPS. */
	public static boolean start(MinecraftServer server, int expectedPlayers,
			int requestedTicks, String label, boolean paceRealTime) {
		if (server == null || SESSIONS.containsKey(server)) return false;
		int ticks = Math.clamp(requestedTicks, 200, 216_000);
		String safeLabel = sanitize(label);
		SESSIONS.put(server, new Session(Math.clamp(expectedPlayers, 0, 1_000), ticks,
				safeLabel, paceRealTime));
		return true;
	}

	/** Captures the start boundary before vanilla and mod server tick work. */
	public static void startTick(MinecraftServer server) {
		Session session = SESSIONS.get(server);
		if (session == null) return;
		long currentTick = server.getTickCount();
		if (!isNewLogicalTick(session.logicalTick, currentTick)) return;
		session.logicalTick = currentTick;
		session.tickStarted = System.nanoTime();
	}

	/** Captures the end boundary and publishes evidence after the requested duration. */
	public static void endTick(MinecraftServer server) {
		Session session = SESSIONS.get(server);
		if (session == null || session.tickStarted == 0L
				|| !isNewLogicalTick(session.sampledTick, session.logicalTick)) return;
		session.sampledTick = session.logicalTick;
		long elapsed = Math.max(0L, System.nanoTime() - session.tickStarted);
		session.durations.add(elapsed);
		var work = ServerRuntimeMetrics.snapshot(server);
		session.peakParticles = Math.max(session.peakParticles, work.particles());
		session.peakPackets = Math.max(session.peakPackets, work.packets());
		session.peakInspections = Math.max(session.peakInspections, work.entityInspections());
		if (session.paceRealTime) waitUntil(profileDeadlineNanos(
				session.startedAt, session.durations.size()));
		if (session.durations.size() >= session.requestedTicks) finish(server, session);
	}

	/** Records one real server-authoritative action attempted by the profiling workload. */
	public static void recordAction(MinecraftServer server, boolean successful) {
		Session session = SESSIONS.get(server);
		if (session == null) return;
		session.attemptedActions++;
		if (successful) session.successfulActions++;
	}

	public static Status status(MinecraftServer server) {
		Session session = SESSIONS.get(server);
		if (session == null) return new Status(false, 0,
				server == null ? 0 : server.getPlayerCount(), 0, 0, "none");
		return new Status(true, session.expectedPlayers, server.getPlayerCount(),
				session.durations.size(), session.requestedTicks, session.label);
	}

	/** Stops without publishing a partial result, for orderly server shutdown. */
	public static void cancel(MinecraftServer server) {
		Session session = SESSIONS.remove(server);
		if (session != null) session.recording.close();
	}

	public static double percentileMs(List<Long> durationsNanos, double percentile) {
		if (durationsNanos == null || durationsNanos.isEmpty()) return 0.0;
		List<Long> sorted = durationsNanos.stream().sorted(Comparator.naturalOrder()).toList();
		double bounded = Math.clamp(percentile, 0.0, 1.0);
		int index = Math.max(0, (int) Math.ceil(sorted.size() * bounded) - 1);
		return sorted.get(index) / 1_000_000.0;
	}

	/** Cumulative 20 TPS deadline; cumulative pacing prevents per-tick drift. */
	public static long profileDeadlineNanos(long startedAt, int sampledTicks) {
		return startedAt + (long) Math.max(0, sampledTicks) * TARGET_TICK_NANOS;
	}

	/** True only once when a monotonically increasing logical server tick arrives. */
	public static boolean isNewLogicalTick(long lastTick, long currentTick) {
		return currentTick > lastTick;
	}

	private static void waitUntil(long deadline) {
		long remaining;
		while ((remaining = deadline - System.nanoTime()) > 0L) {
			LockSupport.parkNanos(remaining);
		}
	}

	private static Recording createRecording(String label) {
		Recording recording = new Recording();
		recording.setName("POWERS connected-player profile " + label);
		recording.enable("jdk.CPULoad").withPeriod(Duration.ofSeconds(1));
		recording.enable("jdk.GarbageCollection");
		recording.enable("jdk.ObjectAllocationSample").with("throttle", "medium");
		recording.enable("jdk.JavaMonitorWait").withThreshold(Duration.ofMillis(10));
		recording.start();
		return recording;
	}

	private static void finish(MinecraftServer server, Session session) {
		SESSIONS.remove(server);
		double wallSeconds = Math.max(0L, System.nanoTime() - session.startedAt)
				/ 1_000_000_000.0;
		session.recording.stop();
		Path directory = Path.of("profiles").toAbsolutePath().normalize();
		Path trace = directory.resolve("connected-" + session.label + ".jfr");
		Path report = directory.resolve("connected-" + session.label + ".json");
		double p95Mspt = percentileMs(session.durations, 0.95);
		double p99Mspt = percentileMs(session.durations, 0.99);
		try {
			Files.createDirectories(directory);
			session.recording.dump(trace);
			String json = "{\n"
					+ "  \"schema\": 1,\n"
					+ "  \"label\": \"" + session.label + "\",\n"
					+ "  \"expected_players\": " + session.expectedPlayers + ",\n"
					+ "  \"connected_players_end\": " + server.getPlayerCount() + ",\n"
					+ "  \"ticks\": " + session.durations.size() + ",\n"
					+ "  \"wall_seconds\": " + wallSeconds + ",\n"
					+ "  \"p95_mspt\": " + p95Mspt + ",\n"
					+ "  \"p99_mspt\": " + p99Mspt + ",\n"
					+ "  \"attempted_actions\": " + session.attemptedActions + ",\n"
					+ "  \"successful_actions\": " + session.successfulActions + ",\n"
					+ "  \"peak_particles_per_tick\": " + session.peakParticles + ",\n"
					+ "  \"peak_packets_per_tick\": " + session.peakPackets + ",\n"
					+ "  \"peak_entity_inspections_per_tick\": " + session.peakInspections + ",\n"
					+ "  \"jfr\": \"" + trace.getFileName() + "\"\n"
					+ "}\n";
			Files.writeString(report, json);
			PowersMod.LOGGER.info("Completed POWERS profile {}: p95={} ms, p99={} ms, report={}",
					session.label, p95Mspt, p99Mspt, report);
		} catch (IOException exception) {
			PowersMod.LOGGER.error("Could not publish POWERS profile {}", session.label, exception);
		} finally {
			session.recording.close();
		}
	}

	private static String sanitize(String label) {
		if (label == null) return "profile";
		String safe = label.toLowerCase(java.util.Locale.ROOT)
				.replaceAll("[^a-z0-9._-]", "_");
		return safe.isBlank() ? "profile" : safe.substring(0, Math.min(48, safe.length()));
	}
}
