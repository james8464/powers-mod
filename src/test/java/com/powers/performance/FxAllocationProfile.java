package com.powers.performance;

import com.powers.fx.BeamFxStyle;
import com.powers.fx.FxGeometry;
import com.powers.magic.fx.FxMotif;
import com.powers.magic.fx.FxOrientation;
import com.powers.network.FxPayloadBatch;
import com.powers.network.MagicFxPackets;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/** Exact repeatable mass-combat workload used for PERF-006 before/after evidence. */
final class FxAllocationProfile {
	private static final int VIEWERS_PER_OPERATION = 64;
	private static final int POINTS_PER_OPERATION = 48;
	private static volatile long blackholeSink;

	private FxAllocationProfile() {
	}

	static Result measure(int warmupBatches, int measuredBatches, int operationsPerBatch) {
		if (warmupBatches < 0 || measuredBatches < 1 || operationsPerBatch < 1) {
			throw new IllegalArgumentException("Allocation profile dimensions must be positive");
		}
		com.sun.management.ThreadMXBean allocations = allocationBean();
		FxGeometry.TransformBuffer transform = new FxGeometry.TransformBuffer()
				.configure(1.35, FxOrientation.BILLBOARD, 0.72);
		long blackhole = 0L;
		for (int batch = 0; batch < warmupBatches; batch++) {
			blackhole = mix(blackhole,
					executeBatch(transform, batch * operationsPerBatch, operationsPerBatch));
		}
		long[] nanos = new long[Math.multiplyExact(measuredBatches, operationsPerBatch)];
		long threadId = Thread.currentThread().threadId();
		long allocatedBefore = allocations.getThreadAllocatedBytes(threadId);
		int sample = 0;
		for (int batch = 0; batch < measuredBatches; batch++) {
			for (int operation = 0; operation < operationsPerBatch; operation++) {
				long started = System.nanoTime();
				blackhole = mix(blackhole, executeOperation(transform,
						(long) batch * operationsPerBatch + operation + 1_000_000L));
				nanos[sample++] = Math.max(1L, System.nanoTime() - started);
			}
		}
		long allocatedBytes = allocations.getThreadAllocatedBytes(threadId) - allocatedBefore;
		blackholeSink = blackhole;
		Arrays.sort(nanos);
		long p99 = nanos[Math.min(nanos.length - 1, (int) Math.ceil(nanos.length * 0.99) - 1)];
		return new Result(nanos.length, allocatedBytes,
				allocatedBytes / (double) nanos.length, p99,
				FxGeometry.poolSize(), 0, blackholeSink);
	}

	private static long executeBatch(FxGeometry.TransformBuffer transform,
			long firstOperation, int count) {
		long blackhole = 0L;
		for (int operation = 0; operation < count; operation++) {
			blackhole = mix(blackhole, executeOperation(transform, firstOperation + operation));
		}
		return blackhole;
	}

	private static long executeOperation(FxGeometry.TransformBuffer transform, long eventId) {
		var points = FxGeometry.points(FxMotif.SPIRAL, (int) (eventId & 15L), 4,
				POINTS_PER_OPERATION);
		long blackhole = eventId;
		for (int index = 0; index < points.size(); index++) {
			transform.apply(points.get(index));
			blackhole = mix(blackhole, Double.doubleToRawLongBits(
					transform.x() + transform.y() + transform.z()));
		}
		FxPayloadBatch.Beam payloads = FxPayloadBatch.beam(eventId, BeamFxStyle.COLORED,
				0.0, 64.0, 0.0, 24.0, 64.0, 0.0, 0x7DEBFF);
		for (int viewer = 0; viewer < VIEWERS_PER_OPERATION; viewer++) {
			MagicFxPackets.BeamFxPayload payload = payloads.forCount(32);
			blackhole = mix(blackhole, System.identityHashCode(payload));
		}
		return blackhole;
	}

	private static long mix(long current, long value) {
		return Long.rotateLeft(current, 11) + value + 0x9E3779B97F4A7C15L;
	}

	private static com.sun.management.ThreadMXBean allocationBean() {
		var bean = ManagementFactory.getThreadMXBean();
		if (!(bean instanceof com.sun.management.ThreadMXBean allocations)
				|| !allocations.isThreadAllocatedMemorySupported()) {
			throw new IllegalStateException("Java runtime does not expose per-thread allocation bytes");
		}
		if (!allocations.isThreadAllocatedMemoryEnabled()) {
			allocations.setThreadAllocatedMemoryEnabled(true);
		}
		return allocations;
	}

	private static String commit() throws IOException, InterruptedException {
		Process process = new ProcessBuilder("git", "rev-parse", "HEAD")
				.redirectErrorStream(true).start();
		String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
		if (process.waitFor() != 0 || output.isBlank()) {
			throw new IOException("Could not resolve exact Git commit: " + output);
		}
		return output;
	}

	public static void main(String[] args) throws Exception {
		if (args.length != 2) throw new IllegalArgumentException("Expected output path and label");
		Path output = Path.of(args[0]);
		String label = args[1];
		Result result = measure(20, 200, 128);
		Files.createDirectories(output.getParent());
		Files.writeString(output, result.json(label, commit()), StandardCharsets.UTF_8);
		System.out.print(result.json(label, commit()));
	}

	record Result(int operations, long allocatedBytes, double allocatedBytesPerOperation,
			long p99NanosPerOperation, int geometryEntries, int payloadEntries, long blackhole) {
		String json(String label, String commit) {
			return "{\n"
					+ "  \"schema\": 1,\n"
					+ "  \"label\": \"" + label + "\",\n"
					+ "  \"commit\": \"" + commit + "\",\n"
					+ "  \"java\": \"" + System.getProperty("java.version") + "\",\n"
					+ "  \"operations\": " + operations + ",\n"
					+ "  \"viewers_per_operation\": " + VIEWERS_PER_OPERATION + ",\n"
					+ "  \"points_per_operation\": " + POINTS_PER_OPERATION + ",\n"
					+ "  \"allocated_bytes\": " + allocatedBytes + ",\n"
					+ "  \"allocated_bytes_per_operation\": "
					+ String.format(java.util.Locale.ROOT, "%.3f", allocatedBytesPerOperation) + ",\n"
					+ "  \"p99_nanos_per_operation\": " + p99NanosPerOperation + ",\n"
					+ "  \"geometry_entries\": " + geometryEntries + ",\n"
					+ "  \"payload_entries\": " + payloadEntries + ",\n"
					+ "  \"blackhole\": " + blackhole + "\n"
					+ "}\n";
		}
	}
}
