package com.powers.network;

import com.powers.fx.BeamFxStyle;
import com.powers.fx.ShapeFxKind;
import com.powers.magic.fx.MagicFxKind;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MagicFxWireSizeTest {
	static {
		net.minecraft.SharedConstants.tryDetectVersion();
		net.minecraft.server.Bootstrap.bootStrap();
	}

	@Test
	void resolvesTheExactMinecraft262ClientboundCustomPayloadPacketId() {
		assertEquals(24, SemanticFxTransport.playCustomPayloadPacketId());
	}

	@Test
	void reportsTheExactPayloadBodySizeWrittenByEachCodec() {
		var magic = new MagicFxPackets.MagicFxPayload(MagicFxKind.CAST, 1L,
				"beam", "powers:cast", 1.0, 2.0, 3.0,
				0x112233, 0x445566, 7, 2, 4);
		var beam = new MagicFxPackets.BeamFxPayload(1L, BeamFxStyle.ELECTRIC,
				0.0, 1.0, 2.0, 3.0, 4.0, 5.0, 16, 0xABCDEF);
		var shape = new MagicFxPackets.ShapeFxPayload(1L, ShapeFxKind.RING,
				0.0, 1.0, 2.0, 3.0, 0.0, 24, 0xABCDEF, 0.5);

		assertEquals(57, MagicFxPackets.encodedBodyBytes(magic));
		assertEquals(55, MagicFxPackets.encodedBodyBytes(beam));
		assertEquals(55, MagicFxPackets.encodedBodyBytes(shape));
	}

	@Test
	void exposesAResettableCaptureSnapshotWithoutResettingOtherNetworkState() {
		MagicFxPackets.resetFxTrafficMetrics();
		assertEquals(new FxPacketCoalescer.TrafficSnapshot(0, 0, 0, 0),
				MagicFxPackets.fxTrafficSnapshot());
	}

	@Test
	void mixedSemanticBatchCodecPreservesEntryOrderAndPayloads() {
		var magic = new MagicFxPackets.MagicFxPayload(MagicFxKind.CAST, 9L,
				"rune", "powers:rune_hum", 1.0, 2.0, 3.0,
				0x112233, 0x445566, 7, 2, 4);
		var beam = new MagicFxPackets.BeamFxPayload(10L, BeamFxStyle.ELECTRIC,
				0.0, 1.0, 2.0, 3.0, 4.0, 5.0, 16, 0xABCDEF);
		var shape = new MagicFxPackets.ShapeFxPayload(11L, ShapeFxKind.RING,
				0.0, 1.0, 2.0, 3.0, 0.0, 24, 0xABCDEF, 0.5);
		var expected = new MagicFxPackets.SemanticFxBatchPayload(java.util.List.of(
				MagicFxPackets.BatchEntry.magic(magic),
				MagicFxPackets.BatchEntry.beam(beam),
				MagicFxPackets.BatchEntry.shape(shape)));
		var raw = Unpooled.buffer();
		try {
			var buffer = new RegistryFriendlyByteBuf(raw, RegistryAccess.EMPTY);
			MagicFxPackets.SemanticFxBatchPayload.STREAM_CODEC.encode(buffer, expected);
			assertEquals(expected,
					MagicFxPackets.SemanticFxBatchPayload.STREAM_CODEC.decode(buffer));
		} finally {
			raw.release();
		}
	}

	@Test
	void transportPlanUsesExactFramesAndFallsBackWhenBatchSupportIsAbsent() {
		var entries = java.util.stream.IntStream.range(0, 8)
				.mapToObj(index -> MagicFxPackets.BatchEntry.beam(
						new MagicFxPackets.BeamFxPayload(index + 1L, BeamFxStyle.ELECTRIC,
								0.0, 1.0, 2.0, 3.0, 4.0, 5.0, 16, 0xABCDEF)))
				.toList();

		var supported = MagicFxPackets.transportPlan(entries, true);
		assertTrue(supported.batch());
		assertTrue(supported.batchWireBytes() < supported.individualWireBytes());

		var unsupported = MagicFxPackets.transportPlan(entries, false);
		assertFalse(unsupported.batch());
		assertEquals(entries, unsupported.entries());
	}
}
