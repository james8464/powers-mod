package com.powers.magic.fx;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.powers.magic.fx.MagicFxKind.CAST;
import static com.powers.magic.fx.MagicFxKind.INTERACTION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Protects the readable timing and accessibility rules of semantic magic FX. */
class FxChoreographyTest {
	@Test
	void castsUseACompactFourBeatCeremony() {
		assertEquals(FxBeat.ANTICIPATION, frame(CAST, 0, false).beat());
		assertTrue(FxChoreography.frame(CAST, 1, false).isEmpty());
		assertEquals(FxBeat.RELEASE, frame(CAST, 3, false).beat());
		assertEquals(FxBeat.IMPACT, frame(CAST, 7, false).beat());
		assertEquals(FxBeat.AFTERMATH, frame(CAST, 13, false).beat());
		assertTrue(FxChoreography.finished(CAST, 17));
	}

	@Test
	void interactionsRetainTheirLongerCollisionRhythm() {
		assertEquals(FxBeat.ANTICIPATION, frame(INTERACTION, 0, false).beat());
		assertEquals(FxBeat.RELEASE, frame(INTERACTION, 4, false).beat());
		assertEquals(FxBeat.IMPACT, frame(INTERACTION, 8, false).beat());
		assertEquals(FxBeat.AFTERMATH, frame(INTERACTION, 15, false).beat());
		assertTrue(FxChoreography.finished(INTERACTION, 18));
	}

	@Test
	void impactActuallyExpandsBeyondAnticipation() {
		FxFrame anticipation = frame(CAST, 0, false);
		FxFrame impact = frame(CAST, 7, false);

		assertTrue(impact.geometryScale() > anticipation.geometryScale());
		assertTrue(impact.budgetScale() > anticipation.budgetScale());
	}

	@Test
	void reducedMotionClampsExpansionAndVelocity() {
		FxFrame normal = frame(CAST, 7, false);
		FxFrame reduced = frame(CAST, 7, true);

		assertTrue(reduced.geometryScale() < normal.geometryScale());
		assertTrue(reduced.geometryScale() <= 0.85);
		assertTrue(reduced.velocityScale() <= 0.25);
		assertEquals(FxMotif.RING, frame(CAST, 13, true).motifOverride().orElseThrow());
	}

	@Test
	void negativeAgesAreRejectedAsProgrammingErrors() {
		assertThrows(IllegalArgumentException.class, () -> FxChoreography.frame(CAST, -1, false));
		assertThrows(IllegalArgumentException.class, () -> FxChoreography.finished(CAST, -1));
	}

	@Test
	void framesRejectNonFiniteOrNegativeScales() {
		assertThrows(IllegalArgumentException.class, () -> new FxFrame(FxBeat.IMPACT,
				Optional.empty(), Double.NaN, 1.0, 1.0));
		assertThrows(IllegalArgumentException.class, () -> new FxFrame(FxBeat.IMPACT,
				Optional.empty(), 1.0, -1.0, 1.0));
	}

	private static FxFrame frame(MagicFxKind kind, int age, boolean reducedMotion) {
		return FxChoreography.frame(kind, age, reducedMotion).orElseThrow();
	}
}
