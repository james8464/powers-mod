package com.powers.network;

import java.nio.charset.StandardCharsets;

/** Computes exact semantic-FX payload body sizes for transport planning. */
final class FxPacketWireSize {
	private FxPacketWireSize() {
	}

	static int encodedBodyBytes(MagicFxPackets.MagicFxPayload payload) {
		return varIntBytes(payload.kind().networkId()) + varLongBytes(payload.eventId())
				+ stringBytes(payload.motif()) + stringBytes(payload.sound())
				+ Double.BYTES * 3 + Integer.BYTES * 3
				+ varIntBytes(payload.intensity()) + varIntBytes(payload.genericBeatCount()) + 1;
	}

	static int encodedBodyBytes(MagicFxPackets.BeamFxPayload payload) {
		return varLongBytes(payload.eventId()) + varIntBytes(payload.style().networkId())
				+ Double.BYTES * 6 + varIntBytes(payload.count()) + Integer.BYTES + 1;
	}

	static int encodedBodyBytes(MagicFxPackets.ShapeFxPayload payload) {
		return varLongBytes(payload.eventId()) + varIntBytes(payload.kind().networkId())
				+ Double.BYTES * 5 + varIntBytes(payload.count())
				+ Integer.BYTES + Double.BYTES + 1;
	}

	private static int stringBytes(String value) {
		int bytes = value.getBytes(StandardCharsets.UTF_8).length;
		return varIntBytes(bytes) + bytes;
	}

	private static int varIntBytes(int value) {
		int bytes = 1;
		while ((value & ~0x7F) != 0) {
			bytes++;
			value >>>= 7;
		}
		return bytes;
	}

	private static int varLongBytes(long value) {
		int bytes = 1;
		while ((value & ~0x7FL) != 0L) {
			bytes++;
			value >>>= 7;
		}
		return bytes;
	}
}
