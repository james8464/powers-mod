package com.powers.magic.fx;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Contract tests for the compact semantic presentation layer. */
class MagicFxServiceTest {
	@Test
	void majorSequenceContainsEveryReadableBeat() {
		assertEquals(EnumSet.allOf(FxBeat.class), EnumSet.copyOf(FxSequence.major().beats()));
	}

	@Test
	void duplicatePairCellTickProducesOneEvent() {
		List<MagicFxEvent> sink = new ArrayList<>();
		MagicFxService service = new MagicFxService(sink::add);
		MagicFxEvent event = MagicFxEvent.interaction(12L, "steam", "rift_open",
				1.0, 2.0, 3.0, 0xFF3300, 0x88CCFF, 42, 4);
		service.emit("pair@cell@12", event);
		service.emit("pair@cell@12", event);
		assertEquals(1, sink.size());
	}

	@Test
	void eventIsSemanticAndBoundedInsteadOfCarryingParticleArrays() {
		MagicFxEvent event = MagicFxEvent.interaction(9L, "violent_interference", "interaction_clash",
				0.0, 64.0, 0.0, 0xFF0000, 0x5500AA, 99, 5);
		assertTrue(event.estimatedWireBytes() < 160);
		assertTrue(event.intensity() <= MagicFxEvent.MAX_INTENSITY);
	}
}
