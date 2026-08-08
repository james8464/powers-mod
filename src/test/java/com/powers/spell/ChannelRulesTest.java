package com.powers.spell;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChannelRulesTest {
	@Test
	void channelCompletesOnlyAtItsDeadline() {
		ChannelState state = new ChannelState(100, 0, 64, 0, "book", false);
		assertEquals(ChannelStatus.CHANNELING,
				ChannelRules.status(state, 99, 0.2, 64, 0.2, true, false));
		assertEquals(ChannelStatus.COMPLETE,
				ChannelRules.status(state, 100, 0.2, 64, 0.2, true, false));
	}

	@Test
	void damageMovementBookSwapAndDampeningInterruptRituals() {
		ChannelState state = new ChannelState(100, 0, 64, 0, "book", false);
		assertEquals(ChannelStatus.INTERRUPTED,
				ChannelRules.status(state, 50, 1.1, 64, 0, true, false));
		assertEquals(ChannelStatus.INTERRUPTED,
				ChannelRules.status(state, 50, 0, 64, 0, false, false));
		assertEquals(ChannelStatus.INTERRUPTED,
				ChannelRules.status(state, 50, 0, 64, 0, true, true));
		assertEquals(ChannelStatus.INTERRUPTED,
				ChannelRules.status(state.withDamaged(true), 50, 0, 64, 0, true, false));
	}
}
