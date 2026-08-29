package com.powers.magic.runtime;

import com.powers.time.ControlTick;
import com.powers.time.WorldTick;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MagicPresenceDeadlineTest {
	@Test
	void authoredDurationUsesWorldTimeWhenWorldAndControlClocksAreFarApart() {
		WorldTick matureWorld = WorldTick.at(2_000_000L);

		assertEquals(WorldTick.at(2_000_200L),
				MagicPresenceDeadline.after(matureWorld, 200L));
	}

	@Test
	void controlOwnedAbilityConvertsOnlyItsRemainingDurationIntoWorldTime() {
		WorldTick matureWorld = WorldTick.at(2_000_000L);
		ControlTick restartLocalControl = ControlTick.at(40L);

		assertEquals(WorldTick.at(2_000_160L), MagicPresenceDeadline.fromControlRemaining(
				matureWorld, restartLocalControl, ControlTick.at(200L)));
		assertEquals(WorldTick.at(2_000_001L), MagicPresenceDeadline.fromControlRemaining(
				matureWorld, restartLocalControl, ControlTick.at(39L)));
	}
}
