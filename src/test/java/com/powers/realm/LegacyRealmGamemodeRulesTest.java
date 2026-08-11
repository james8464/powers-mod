package com.powers.realm;

import net.minecraft.world.level.GameType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Regression coverage for saves created by the retired mindscape Adventure coercion. */
class LegacyRealmGamemodeRulesTest {
	@Test
	void restoresAValidSnapshotOnlyWhenTheOldCodeLeftThePlayerInAdventure() {
		var stranded = LegacyRealmGamemodeRules.decide("survival", GameType.ADVENTURE);
		assertEquals(GameType.SURVIVAL, stranded.restore());
		assertTrue(stranded.clearSnapshot());

		var legitimateCreative = LegacyRealmGamemodeRules.decide("survival", GameType.CREATIVE);
		assertNull(legitimateCreative.restore());
		assertTrue(legitimateCreative.clearSnapshot());
	}

	@Test
	void malformedAndEmptySnapshotsNeverOverrideTheCurrentMode() {
		var malformed = LegacyRealmGamemodeRules.decide("builder_mode", GameType.ADVENTURE);
		assertNull(malformed.restore());
		assertTrue(malformed.clearSnapshot());

		var absent = LegacyRealmGamemodeRules.decide("", GameType.ADVENTURE);
		assertNull(absent.restore());
		assertTrue(!absent.clearSnapshot());
	}
}
