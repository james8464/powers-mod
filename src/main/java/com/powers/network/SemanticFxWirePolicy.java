package com.powers.network;

import java.util.List;
import java.util.Objects;
import java.util.zip.Deflater;

/** Selects batching only when the exact candidate bytes reduce framed network traffic. */
public final class SemanticFxWirePolicy {
	private static final ThreadLocal<Deflater> DEFLATER = ThreadLocal.withInitial(Deflater::new);
	private static final ThreadLocal<byte[]> DEFLATE_BUFFER =
			ThreadLocal.withInitial(() -> new byte[8192]);

	private SemanticFxWirePolicy() {
	}

	public record Decision(boolean batch, int individualWireBytes, int batchWireBytes) {
	}

	public static Decision decide(List<byte[]> individualFrames, byte[] batchFrame,
			int compressionThreshold) {
		Objects.requireNonNull(individualFrames, "individualFrames");
		Objects.requireNonNull(batchFrame, "batchFrame");
		if (individualFrames.isEmpty()) return new Decision(false, 0, framedBytes(batchFrame,
				compressionThreshold));
		int individualBytes = 0;
		for (byte[] frame : individualFrames) {
			individualBytes = saturatedAdd(individualBytes,
					framedBytes(Objects.requireNonNull(frame, "individualFrame"), compressionThreshold));
		}
		int batchBytes = framedBytes(batchFrame, compressionThreshold);
		return new Decision(individualFrames.size() > 1 && batchBytes < individualBytes,
				individualBytes, batchBytes);
	}

	static int framedBytes(byte[] packet, int compressionThreshold) {
		int bodyBytes;
		if (compressionThreshold < 0) {
			bodyBytes = packet.length;
		} else if (packet.length < compressionThreshold) {
			bodyBytes = 1 + packet.length;
		} else {
			bodyBytes = varIntBytes(packet.length) + deflatedBytes(packet);
		}
		return saturatedAdd(varIntBytes(bodyBytes), bodyBytes);
	}

	private static int deflatedBytes(byte[] packet) {
		Deflater deflater = DEFLATER.get();
		try {
			deflater.setInput(packet);
			deflater.finish();
			byte[] buffer = DEFLATE_BUFFER.get();
			int bytes = 0;
			while (!deflater.finished()) bytes = saturatedAdd(bytes, deflater.deflate(buffer));
			return bytes;
		} finally {
			deflater.reset();
		}
	}

	private static int varIntBytes(int value) {
		int bytes = 1;
		while ((value & ~0x7F) != 0) {
			bytes++;
			value >>>= 7;
		}
		return bytes;
	}

	private static int saturatedAdd(int left, int right) {
		return Integer.MAX_VALUE - left < right ? Integer.MAX_VALUE : left + right;
	}
}
