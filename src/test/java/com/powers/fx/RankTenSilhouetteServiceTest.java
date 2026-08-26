package com.powers.fx;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Proves the immutable policy used by the real bounded ServerPlayer delivery path. */
class RankTenSilhouetteServiceTest {
	private static final String OVERWORLD = "minecraft:overworld";
	private static final UUID CASTER = UUID.fromString("7332af41-05ca-423c-b1e8-11c1fece4377");

	@Test
	void filtersOnlyLiveSupportedSameDimensionObserversInsideInclusiveRange() {
		List<RankTenSilhouetteService.ObserverOffer> observers = List.of(
				observer(1, OVERWORLD, 384, 0, true, true),
				observer(2, OVERWORLD, 384.01, 0, true, true),
				observer(3, "powers:dark_realm", 1, 0, true, true),
				observer(4, OVERWORLD, 1, 0, false, true),
				observer(5, OVERWORLD, 1, 0, true, false));

		var result = RankTenSilhouetteService.offer(
				RankTenSilhouetteService.initialPolicy(1L), cast(7, CASTER, "fireball"), observers);

		assertTrue(result.accepted());
		assertEquals(List.of(uuid(1)), result.recipients());
		assertEquals(1, result.diagnostics().unsupportedObservers());
		assertEquals(1, result.diagnostics().staleObservers());
		assertEquals(1, result.diagnostics().rangeObservers());
		assertEquals(1, result.diagnostics().dimensionObservers());
		assertEquals(0, result.diagnostics().chunkTicketsRequested());
	}

	@Test
	void canonicalRankAndSameTickCoalescingAreFailClosed() {
		var state = RankTenSilhouetteService.initialPolicy(1L);
		var belowRank = RankTenSilhouetteService.offer(state,
				new RankTenSilhouetteService.CastOffer(4, CASTER, 9, "fireball", OVERWORLD,
						0, 0, 0, 0, 0, 0, 1), List.of());
		var unknown = RankTenSilhouetteService.offer(belowRank.state(), cast(4, CASTER, "not_a_power"), List.of());
		var alias = RankTenSilhouetteService.offer(unknown.state(), cast(4, CASTER, "size_morph"), List.of());
		var duplicateAlias = RankTenSilhouetteService.offer(alias.state(), cast(4, CASTER, "size_shift"), List.of());
		var differentProfile = RankTenSilhouetteService.offer(duplicateAlias.state(), cast(4, CASTER, "flight"), List.of());

		assertFalse(belowRank.accepted());
		assertFalse(unknown.accepted());
		assertTrue(alias.accepted());
		assertEquals("size_shift", alias.profile().powerId());
		assertFalse(duplicateAlias.accepted());
		assertTrue(differentProfile.accepted());
		assertEquals(1, differentProfile.diagnostics().belowRank());
		assertEquals(1, differentProfile.diagnostics().invalidProfiles());
		assertEquals(1, differentProfile.diagnostics().coalescedEvents());
	}

	@Test
	void hardGlobalCapResetsOnNextTickWithoutCreatingTickets() {
		var state = RankTenSilhouetteService.initialPolicy(1L);
		List<RankTenSilhouetteService.Decision> decisions = new ArrayList<>();
		for (int index = 0; index < 33; index++) {
			var result = RankTenSilhouetteService.offer(state, cast(12, uuid(index + 100), "flight"), List.of());
			decisions.add(result);
			state = result.state();
		}
		assertEquals(32, decisions.stream().filter(RankTenSilhouetteService.Decision::accepted).count());
		assertFalse(decisions.get(32).accepted());
		assertEquals(1, state.diagnostics().budgetRejected());
		assertEquals(0, state.diagnostics().chunkTicketsRequested());

		var nextTick = RankTenSilhouetteService.offer(state, cast(13, uuid(200), "flight"), List.of());
		assertTrue(nextTick.accepted());
		assertEquals(33, nextTick.diagnostics().acceptedEvents());
	}

	@Test
	void eventIdExhaustionRejectsPresentationWithoutMutatingGameplayFacingInputs() {
		var state = RankTenSilhouetteService.initialPolicy(Long.MAX_VALUE);
		var result = RankTenSilhouetteService.offer(state, cast(1, CASTER, "double_health"), List.of());

		assertFalse(result.accepted());
		assertEquals(1, result.diagnostics().exhaustedEvents());
		assertEquals(Long.MAX_VALUE, result.state().nextEventId());
		assertEquals(0, result.diagnostics().acceptedEvents());
	}

	private static RankTenSilhouetteService.CastOffer cast(long tick, UUID caster, String power) {
		return new RankTenSilhouetteService.CastOffer(tick, caster, 10, power, OVERWORLD,
				0, 0, 0, 45, -12, 1, 0xC0FFEE);
	}

	private static RankTenSilhouetteService.ObserverOffer observer(int id, String dimension,
			double x, double z, boolean supported, boolean live) {
		return new RankTenSilhouetteService.ObserverOffer(uuid(id), dimension, x, 0, z, supported, live);
	}

	private static UUID uuid(int value) {
		return new UUID(0xABCDL, value);
	}
}
