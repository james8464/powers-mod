package com.powers.spell;

import com.powers.progression.ScaledMagicValues;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpellCastValuesTest {
	@Test
	void neutralProfilePreservesAllDocumentedSpellBaselines() {
		SpellCastValues values = SpellCastValues.from(scaling(1.0, 1.0, 1.0), false);

		assertEquals(32.0, values.targetRange());
		assertEquals(600, values.durationTicks());
		assertEquals(6.0f, values.damage());
		assertEquals(7.0, values.fieldRadius());
		assertEquals(8.0, values.purificationRadius());
		assertEquals(2.5, values.banishForce());
		assertEquals(6, values.fireSeconds());
		assertEquals(900, values.wardSuppressionTicks());
		assertEquals(40, values.channelTicks(40));
	}

	@Test
	void rankAndAmplificationComposeOnceWithFiniteCaps() {
		SpellCastValues values = SpellCastValues.from(scaling(1.3, 1.25, 1.4), true);

		assertEquals(40.0, values.targetRange());
		assertEquals(1260, values.durationTicks());
		assertEquals(11.7f, values.damage(), 0.001f);
		assertEquals(10.5, values.fieldRadius());
		assertEquals(15.0, values.purificationRadius());
		assertEquals(5.2, values.banishForce(), 0.001);
		assertEquals(13, values.fireSeconds());
		assertEquals(2520, values.wardSuppressionTicks());
		assertTrue(values.channelTicks(40) < 40);
		assertTrue(values.potencyTier() >= 1);
	}

	private static ScaledMagicValues scaling(double potency, double range, double duration) {
		return new ScaledMagicValues(10, 32.0, 600, 20, 600, 15, Set.of(), 1.0,
				potency, range, duration);
	}
}
