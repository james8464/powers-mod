package com.powers.network;

import com.powers.fx.BeamFxStyle;
import com.powers.fx.ShapeFxKind;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

/** Reuse and isolation contracts for one-event semantic payload fan-out. */
class FxPayloadBatchTest {
	@Test
	void beamBatchAllocatesOnceForRepeatedObserverCount() {
		FxPayloadBatch.Beam batch = FxPayloadBatch.beam(7L, BeamFxStyle.ELECTRIC,
				0.0, 64.0, 0.0, 12.0, 64.0, 0.0, 0x7DEBFF);

		var first = batch.forCount(32);
		assertSame(first, batch.forCount(32));
		assertEquals(32, first.count());
		assertNotSame(first, batch.forCount(24));
		assertSame(first, batch.forCount(32));
	}

	@Test
	void shapeBatchAllocatesOnceForRepeatedObserverCount() {
		FxPayloadBatch.Shape batch = FxPayloadBatch.shape(9L, ShapeFxKind.RUNE,
				1.0, 2.0, 3.0, 4.0, 1.5, 0xB36BFF, 0.25);

		var first = batch.forCount(48);
		assertSame(first, batch.forCount(48));
		assertEquals(48, first.count());
		assertNotSame(first, batch.forCount(36));
		assertSame(first, batch.forCount(48));
	}

	@Test
	void eventScalePayloadVariantsCarryOnlyTheExplicitDistanceOverride() {
		FxPayloadBatch.Beam beam = FxPayloadBatch.beam(11L, BeamFxStyle.ELECTRIC,
				0.0, 64.0, 0.0, 12.0, 64.0, 0.0, 0x7DEBFF);
		FxPayloadBatch.Shape shape = FxPayloadBatch.shape(12L, ShapeFxKind.RUNE,
				1.0, 2.0, 3.0, 4.0, 1.5, 0xB36BFF, 0.25);

		assertEquals(false, beam.forCount(16, false).overrideDistanceLimiter());
		assertEquals(true, beam.forCount(16, true).overrideDistanceLimiter());
		assertEquals(false, shape.forCount(24, false).overrideDistanceLimiter());
		assertEquals(true, shape.forCount(24, true).overrideDistanceLimiter());
	}

	@Test
	void distanceOverrideDoesNotAliasTheOrdinaryCanonicalVariant() {
		FxPayloadBatch.Shape shape = FxPayloadBatch.shape(13L, ShapeFxKind.RING,
				0.0, 0.0, 0.0, 8.0, 0.0, 0xFFFFFF, 0.0);
		var ordinary = shape.forCount(24, false);
		var distant = shape.forCount(24, true);
		assertNotSame(ordinary, distant);
		assertSame(ordinary, shape.forCount(24, false));
	}
}
