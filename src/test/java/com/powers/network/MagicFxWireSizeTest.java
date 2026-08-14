package com.powers.network;

import com.powers.fx.BeamFxStyle;
import com.powers.fx.ShapeFxKind;
import com.powers.magic.fx.MagicFxKind;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MagicFxWireSizeTest {
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
}
