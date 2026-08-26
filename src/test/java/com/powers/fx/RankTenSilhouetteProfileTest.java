package com.powers.fx;

import com.powers.progression.InnatePowerLevels;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Locks the shared rank-ten catalogue to the authoritative innate registry. */
class RankTenSilhouetteProfileTest {
	@Test
	void catalogueExactlyMatchesAllTwentyThreeInnatesWithUniqueMonochromeOutlines() {
		assertEquals(InnatePowerLevels.powerIds(), RankTenSilhouetteProfile.powerIds());
		assertEquals(23, RankTenSilhouetteProfile.powerIds().size());
		assertEquals(23, RankTenSilhouetteProfile.powerIds().stream()
				.map(id -> RankTenSilhouetteProfile.forPower(id).orElseThrow().primitiveSignature())
				.distinct().count());
		for (String id : RankTenSilhouetteProfile.powerIds()) {
			RankTenSilhouetteProfile profile = RankTenSilhouetteProfile.forPower(id).orElseThrow();
			assertEquals(profile, RankTenSilhouetteProfile.fromNetworkId(profile.networkId()).orElseThrow());
			assertTrue(profile.primitives().size() > 0 && profile.primitives().size() <= 64, id);
			assertTrue(profile.primitives().stream().allMatch(
					RankTenSilhouetteProfile.Primitive::finite), id);
		}
	}

	@Test
	void aliasesAreCanonicalAndUnknownPowersOrNetworkIdsNeverAcquireAnIdentity() {
		assertEquals(RankTenSilhouetteProfile.forPower("size_shift"),
				RankTenSilhouetteProfile.forPower("size_morph"));
		assertTrue(RankTenSilhouetteProfile.forPower("not_an_innate").isEmpty());
		assertTrue(RankTenSilhouetteProfile.fromNetworkId(-1).isEmpty());
		assertTrue(RankTenSilhouetteProfile.fromNetworkId(23).isEmpty());
	}

	@Test
	void palettesAreClosedLegalAndDoNotChangeThePrimitiveIdentity() {
		for (String id : RankTenSilhouetteProfile.powerIds()) {
			RankTenSilhouetteProfile profile = RankTenSilhouetteProfile.forPower(id).orElseThrow();
			RankTenSilhouetteProfile.Palette radiant = profile.alignmentPalette(true);
			RankTenSilhouetteProfile.Palette darkness = profile.alignmentPalette(false);
			assertTrue(radiant.legal());
			assertTrue(darkness.legal());
			assertFalse(radiant.equals(darkness));
			assertEquals(profile.primitiveSignature(), profile.primitiveSignature());
		}
	}

	@Test
	void primitiveRecordsRejectNonFiniteOrUnboundedDefinitionsAndRemainImmutable() {
		assertThrows(IllegalArgumentException.class,
				() -> new RankTenSilhouetteProfile.Segment(Double.NaN, 0, 0, 1, 0.1));
		assertThrows(IllegalArgumentException.class,
				() -> new RankTenSilhouetteProfile.Ring(0, 0, 0, 8, 0.1));
		assertThrows(IllegalArgumentException.class,
				() -> new RankTenSilhouetteProfile.Disc(0, 0, Double.POSITIVE_INFINITY));
		RankTenSilhouetteProfile profile = RankTenSilhouetteProfile.forPower("flight").orElseThrow();
		assertThrows(UnsupportedOperationException.class,
				() -> profile.primitives().add(profile.primitives().getFirst()));
		assertEquals(List.copyOf(profile.primitives()), profile.primitives());
		assertEquals(23, RankTenSilhouetteProfile.powerIds().stream().map(
				id -> RankTenSilhouetteProfile.forPower(id).orElseThrow().networkId())
				.collect(Collectors.toSet()).size());
	}
}
