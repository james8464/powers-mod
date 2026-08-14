package com.powers.network;

import com.powers.fx.BeamFxStyle;
import com.powers.fx.ShapeFxKind;
import com.powers.magic.fx.MagicFxKind;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;

/** Profiles the production mixed-payload codec and byte-selection rule from 64 B through 8 KiB. */
final class FxCompressionProfile {
	private static final List<Integer> TARGETS = List.of(64, 128, 256, 512, 1024, 2048, 4096, 8192);
	private static final List<Integer> THRESHOLDS = List.of(-1, 128, 256, 512);

	private FxCompressionProfile() {
	}

	static Result measure(int warmups, int iterations) {
		if (warmups < 0 || iterations < 1) throw new IllegalArgumentException("Invalid profile size");
		List<Row> rows = new ArrayList<>(TARGETS.size() * THRESHOLDS.size());
		for (int target : TARGETS) {
			List<MagicFxPackets.BatchEntry> entries = entriesForTarget(target);
			for (int threshold : THRESHOLDS) {
				rows.add(measure(target, threshold, entries, warmups, iterations));
			}
		}
		return new Result(List.copyOf(rows));
	}

	private static Row measure(int target, int threshold, List<MagicFxPackets.BatchEntry> entries,
			int warmups, int iterations) {
		for (int index = 0; index < warmups; index++) {
			MagicFxPackets.transportPlan(entries, true, threshold);
			decode(encode(entries));
		}
		long[] decisions = new long[iterations];
		long[] decodes = new long[iterations];
		MagicFxPackets.TransportPlan plan = null;
		byte[] body = encode(entries);
		boolean roundTrip = true;
		for (int index = 0; index < iterations; index++) {
			long started = System.nanoTime();
			plan = MagicFxPackets.transportPlan(entries, true, threshold);
			decisions[index] = Math.max(1L, System.nanoTime() - started);
			started = System.nanoTime();
			roundTrip &= entries.equals(decode(body).entries());
			decodes[index] = Math.max(1L, System.nanoTime() - started);
		}
		byte[] batchFrame = SemanticFxTransport.encodedFrame(new MagicFxPackets.SemanticFxBatchPayload(entries));
		int individualInputBytes = entries.stream().map(SemanticFxTransport::encodedFrame)
				.mapToInt(frame -> frame.length).sum();
		SemanticFxWirePolicy.Decision control = incompressibleControl(batchFrame.length, threshold);
		return new Row(target, threshold, entries.size(), batchFrame.length, individualInputBytes,
				plan.individualWireBytes(), plan.batchWireBytes(), plan.batch(),
				percentSaved(plan.individualWireBytes(), plan.batchWireBytes()),
				p95(decisions), p95(decodes), control.individualWireBytes(),
				control.batchWireBytes(), control.batch(), roundTrip);
	}

	private static List<MagicFxPackets.BatchEntry> entriesForTarget(int target) {
		List<MagicFxPackets.BatchEntry> entries = new ArrayList<>();
		for (int index = 0; index < SemanticFxBatchAccumulator.DEFAULT_MAX_ENTRIES; index++) {
			entries.add(representativeEntry(index));
			int bytes = SemanticFxTransport.encodedFrame(
					new MagicFxPackets.SemanticFxBatchPayload(entries)).length;
			if (bytes >= target) return List.copyOf(entries);
		}
		return List.copyOf(entries);
	}

	private static MagicFxPackets.BatchEntry representativeEntry(int index) {
		long eventId = 0xF0150000L + index;
		return switch (index % 3) {
			case 0 -> MagicFxPackets.BatchEntry.magic(new MagicFxPackets.MagicFxPayload(
					index % 2 == 0 ? MagicFxKind.CAST : MagicFxKind.INTERACTION, eventId,
					"powers:ancient_rune", "powers:rune_hum",
					index * 0.75, 64.0 + index % 5, -index * 0.5,
					0x7136C8, 0xD8C4FF, index * 31, 2 + index % 4, 3 + index % 5));
			case 1 -> MagicFxPackets.BatchEntry.beam(new MagicFxPackets.BeamFxPayload(
					eventId, BeamFxStyle.values()[index % BeamFxStyle.values().length],
					index, 65.0, -index, index + 12.0, 66.5, -index + 4.0,
					16 + index % 24, 0xB8F4FF));
			default -> MagicFxPackets.BatchEntry.shape(new MagicFxPackets.ShapeFxPayload(
					eventId, ShapeFxKind.values()[index % ShapeFxKind.values().length],
					index * 0.5, 64.0, -index * 0.5, 2.0 + index % 12,
					index % 7, 24 + index % 64, 0x7136C8, index * 0.125));
		};
	}

	private static SemanticFxWirePolicy.Decision incompressibleControl(int bytes, int threshold) {
		byte[] random = new byte[bytes];
		new Random(0xF015L + bytes + threshold).nextBytes(random);
		List<byte[]> split = new ArrayList<>();
		for (int offset = 0; offset < random.length; offset += 64) {
			split.add(Arrays.copyOfRange(random, offset, Math.min(random.length, offset + 64)));
		}
		return SemanticFxWirePolicy.decide(split, random, threshold);
	}

	private static byte[] encode(List<MagicFxPackets.BatchEntry> entries) {
		var raw = Unpooled.buffer();
		try {
			var buffer = new RegistryFriendlyByteBuf(raw, RegistryAccess.EMPTY);
			MagicFxPackets.SemanticFxBatchPayload.STREAM_CODEC.encode(buffer,
					new MagicFxPackets.SemanticFxBatchPayload(entries));
			byte[] body = new byte[raw.readableBytes()];
			raw.getBytes(raw.readerIndex(), body);
			return body;
		} finally {
			raw.release();
		}
	}

	private static MagicFxPackets.SemanticFxBatchPayload decode(byte[] body) {
		var raw = Unpooled.wrappedBuffer(body);
		try {
			return MagicFxPackets.SemanticFxBatchPayload.STREAM_CODEC.decode(
					new RegistryFriendlyByteBuf(raw, RegistryAccess.EMPTY));
		} finally {
			raw.release();
		}
	}

	private static double percentSaved(int before, int after) {
		return before == 0 ? 0.0 : (before - after) * 100.0 / before;
	}

	private static long p95(long[] values) {
		Arrays.sort(values);
		return values[Math.min(values.length - 1, (int) Math.ceil(values.length * 0.95) - 1)];
	}

	record Row(int targetBytes, int compressionThreshold, int entryCount, int batchInputBytes,
			int individualInputBytes, int individualWireBytes, int batchWireBytes,
			boolean selected, double savingsPercent, long decisionP95Nanos,
			long decodeP95Nanos, int incompressibleIndividualWireBytes,
			int incompressibleBatchWireBytes, boolean incompressibleControlSelected,
			boolean roundTrip) {
	}

	record Result(List<Row> rows) {
		String json(String commit) {
			StringBuilder json = new StringBuilder("{\n  \"schema\": 2,\n  \"commit\": \"")
					.append(commit).append("\",\n  \"java\": \"")
					.append(System.getProperty("java.version"))
					.append("\",\n  \"selection_criterion\": \"batch_wire_bytes < individual_wire_bytes\",\n  \"rows\": [\n");
			for (int index = 0; index < rows.size(); index++) {
				Row row = rows.get(index);
				json.append(String.format(Locale.ROOT,
						"    {\"target_bytes\":%d,\"compression_threshold\":%d,\"entry_count\":%d,\"batch_input_bytes\":%d,\"individual_input_bytes\":%d,\"individual_wire_bytes\":%d,\"batch_wire_bytes\":%d,\"selected\":%s,\"savings_percent\":%.3f,\"decision_p95_nanos\":%d,\"decode_p95_nanos\":%d,\"incompressible_individual_wire_bytes\":%d,\"incompressible_batch_wire_bytes\":%d,\"incompressible_control_selected\":%s,\"round_trip\":%s}",
						row.targetBytes, row.compressionThreshold, row.entryCount,
						row.batchInputBytes, row.individualInputBytes, row.individualWireBytes,
						row.batchWireBytes, row.selected, row.savingsPercent,
						row.decisionP95Nanos, row.decodeP95Nanos,
						row.incompressibleIndividualWireBytes, row.incompressibleBatchWireBytes,
						row.incompressibleControlSelected, row.roundTrip));
				json.append(index + 1 == rows.size() ? "\n" : ",\n");
			}
			return json.append("  ]\n}\n").toString();
		}
	}

	public static void main(String[] args) throws IOException, InterruptedException {
		if (args.length != 1) throw new IllegalArgumentException("Expected output path");
		net.minecraft.SharedConstants.tryDetectVersion();
		net.minecraft.server.Bootstrap.bootStrap();
		Process git = new ProcessBuilder("git", "rev-parse", "HEAD").redirectErrorStream(true).start();
		String commit = new String(git.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
		if (git.waitFor() != 0 || commit.isBlank()) throw new IOException("Could not resolve commit");
		String json = measure(100, 2_000).json(commit);
		Path output = Path.of(args[0]);
		Files.createDirectories(output.getParent());
		Files.writeString(output, json, StandardCharsets.UTF_8);
		System.out.print(json);
	}
}
