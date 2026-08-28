package com.powers.audio;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.powers.audio.LayeredAudioCue.CELESTIAL_RING;
import static com.powers.audio.LayeredAudioCue.RUNE_HUM;
import static com.powers.audio.LayeredAudioLayer.FAR;
import static com.powers.audio.LayeredAudioLayer.MID;
import static com.powers.audio.LayeredAudioLayer.NEAR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LayeredAudioRulesTest {
	@Test
	void eachProfileUsesInclusiveInnerThresholdsAndRejectsBeyondItsFarRadius() {
		assertProfile(LayeredAudioProfile.INTIMATE, 8.0, 28.0, 72.0);
		assertProfile(LayeredAudioProfile.STANDARD, 12.0, 48.0, 128.0);
		assertProfile(LayeredAudioProfile.WORLD, 20.0, 96.0, 256.0);
	}

	@Test
	void obstructionAdvancesOneLayerAndAppliesExactFalloff() {
		LayeredAudioRules.ResolvedLayer openNear = resolve(RUNE_HUM, 4.0, false, false, 0.8F, 1);
		LayeredAudioRules.ResolvedLayer blockedNear = resolve(RUNE_HUM, 4.0, true, false, 0.8F, 1);
		LayeredAudioRules.ResolvedLayer blockedMid = resolve(RUNE_HUM, 20.0, true, false, 0.8F, 1);
		LayeredAudioRules.ResolvedLayer blockedFar = resolve(RUNE_HUM, 60.0, true, false, 0.8F, 1);

		assertEquals(NEAR, openNear.layer());
		assertEquals(1.0F, openNear.obstructionGain());
		assertEquals(MID, blockedNear.layer());
		assertEquals(FAR, blockedMid.layer());
		assertEquals(FAR, blockedFar.layer());
		assertEquals(0.45F, blockedNear.obstructionGain());
		assertEquals(0.36F, blockedNear.gain(), 0.0001F);
	}

	@Test
	void comfortAlternativeAppliesOnlyToCelestialRing() {
		assertTrue(resolve(CELESTIAL_RING, 4.0, false, true, 0.8F, 1).reducedTinnitus());
		assertFalse(resolve(RUNE_HUM, 4.0, false, true, 0.8F, 1).reducedTinnitus());
		assertFalse(resolve(CELESTIAL_RING, 4.0, false, false, 0.8F, 1).reducedTinnitus());
	}

	@Test
	void concurrentSoundsReceiveBoundedInverseSquareRootHeadroom() {
		assertEquals(0.90F, LayeredAudioRules.headroom(1, 0.90F), 0.0001F);
		assertEquals(0.45F, LayeredAudioRules.headroom(4, 0.90F), 0.0001F);
		assertEquals(0.90F / (float) Math.sqrt(8.0),
				LayeredAudioRules.headroom(99, 0.90F), 0.0001F);
		assertEquals(0.90F, LayeredAudioRules.headroom(0, 2.0F), 0.0001F);
	}

	@Test
	void invalidOrSilentInputsDoNotResolveAndFiniteValuesAreClamped() {
		assertTrue(LayeredAudioRules.resolve(null, 1.0, false, false, 1.0F, 1).isEmpty());
		assertTrue(LayeredAudioRules.resolve(RUNE_HUM, -1.0, false, false, 1.0F, 1).isEmpty());
		assertTrue(LayeredAudioRules.resolve(RUNE_HUM, Double.NaN, false, false, 1.0F, 1).isEmpty());
		assertTrue(LayeredAudioRules.resolve(RUNE_HUM, 1.0, false, false, 0.0F, 1).isEmpty());
		assertTrue(LayeredAudioRules.resolve(RUNE_HUM, 1.0, false, false, Float.NaN, 1).isEmpty());
		assertEquals(0.90F, resolve(RUNE_HUM, 1.0, false, false, 4.0F, 1).gain());
	}

	private static void assertProfile(LayeredAudioProfile profile,
			double near, double mid, double far) {
		assertEquals(Optional.of(NEAR), profile.layer(0.0));
		assertEquals(Optional.of(NEAR), profile.layer(near));
		assertEquals(Optional.of(MID), profile.layer(Math.nextUp(near)));
		assertEquals(Optional.of(MID), profile.layer(mid));
		assertEquals(Optional.of(FAR), profile.layer(Math.nextUp(mid)));
		assertEquals(Optional.of(FAR), profile.layer(far));
		assertEquals(Optional.empty(), profile.layer(Math.nextUp(far)));
		assertEquals(Optional.empty(), profile.layer(-1.0));
		assertEquals(Optional.empty(), profile.layer(Double.NaN));
	}

	private static LayeredAudioRules.ResolvedLayer resolve(LayeredAudioCue cue, double distance,
			boolean obstructed, boolean reducedTinnitus, float gain, int concurrent) {
		return LayeredAudioRules.resolve(cue, distance, obstructed, reducedTinnitus, gain, concurrent)
				.orElseThrow();
	}
}
