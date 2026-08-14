package com.powers.performance;

import com.powers.fx.BeamFxStyle;
import com.powers.fx.FxGeometry;
import com.powers.magic.fx.FxMotif;
import com.powers.magic.fx.FxOrientation;
import com.powers.network.FxPayloadPool;
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
	private static final FxGeometry.Point[] ESCAPE_SINK = new FxGeometry.Point[128];

	private FxAllocationProfile() {
	}

	static Result measure(int warmupBatches, int measuredBatches, int operationsPerBatch) {
		if (warmupBatches < 0 || measuredBatches < 1 || operationsPerBatch < 1) {
			throw new IllegalArgumentException("Allocation profile dimensions must be positive");
		}
		com.sun.management.ThreadMXBean allocations = allocationBean();
		FxPayloadPool payloads = new FxPayloadPool(1_024);
		long blackhole = 0L;
		for (int batch = 0; batch < warmupBatches; batch++) {
			blackhole ^= executeBatch(payloads, batch * operationsPerBatch, operationsPerBatch);
		}
		payloads.clear();
		long[] nanos = new long[Math.multiplyExact(measuredBatches, operationsPerBatch)];
		long threadId = Thread.currentThread().threadId();
		long allocatedBefore = allocations.getThreadAllocatedBytes(threadId);
		int sample = 0;
		for (int batch = 0; batch < measuredBatches; batch++) {
			for (int operation = 0; operation < operationsPerBatch; operation++) {
				long started = System.nanoTime();
				blackhole ^= executeOperation(payloads,
						(long) batch * operationsPerBatch + operation + 1_000_000L);
				nanos[sample++] = Math.max(1L, System.nanoTime() - started);
			}
		}
		long allocatedBytes = allocations.getThreadAllocatedBytes(threadId) - allocatedBefore;
		Arrays.sort(nanos);
		long p99 = nanos[Math.min(nanos.length - 1, (int) Math.ceil(nanos.length * 0.99) - 1)];
		return new Result(nanos.length, allocatedBytes,
				allocatedBytes / (double) nanos.length, p99,
				FxGeometry.poolSize(), payloads.size(), blackhole);
	}

	private static long executeBatch(FxPayloadPool payloads, long firstOperation, int count) {
		long blackhole = 0L;
		for (int operation = 0; operation < count; operation++) {
			blackhole ^= executeOperation(payloads, firstOperation + operation);
		}
		return blackhole;
	}

	private static long executeOperation(FxPayloadPool payloads, long eventId) {
		var points = FxGeometry.points(FxMotif.SPIRAL, (int) (eventId & 15L), 4,
				POINTS_PER_OPERATION);
		long blackhole = eventId;
		for (int index = 0; index < points.size(); index++) {
			FxGeometry.Point scaled = FxGeometry.scale(points.get(index), 1.35);
			FxGeometry.Point transformed = FxGeometry.transform(scaled,
					FxOrientation.BILLBOARD, 0.72);
			ESCAPE_SINK[(int) ((eventId + index) & (ESCAPE_SINK.length - 1))] = transformed;
			blackhole ^= Double.doubleToRawLongBits(transformed.x() + transformed.y()
					+ transformed.z());
		}
		for (int viewer = 0; viewer < VIEWERS_PER_OPERATION; viewer++) {
			var payload = payloads.intern(new MagicFxPackets.BeamFxPayload(
					eventId, BeamFxStyle.COLORED, 0.0, 64.0, 0.0,
					24.0, 64.0, 0.0, 32, 0x7DEBFF));
			blackhole ^= System.identityHashCode(payload);
		}
		return blackhole;
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
