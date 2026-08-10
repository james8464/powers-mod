package com.powers.power;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MagicUseGateTest {
	@Test
	void suppressionReasonsHaveOneStablePriority() {
		assertEquals(MagicUseGate.Block.NONE,
				MagicUseGate.reason(false, false, false));
		assertEquals(MagicUseGate.Block.GLOBAL_TIME_STOP,
				MagicUseGate.reason(true, true, true));
		assertEquals(MagicUseGate.Block.AMETHYST,
				MagicUseGate.reason(false, true, true));
		assertEquals(MagicUseGate.Block.LOCAL_FREEZE,
				MagicUseGate.reason(false, false, true));
	}
}
