package com.powers.magic;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MagicInteractionResolverTest {
	private static MagicActionCatalogue catalogue;
	private static MagicInteractionResolver resolver;

	@BeforeAll
	static void createResolver() {
		catalogue = MagicActionCatalogue.defaults();
		resolver = MagicInteractionResolver.defaults(catalogue);
	}

	@Test
	void everyUnorderedPairIncludingSelfHasMechanicsAndPresentation() {
		var pairs = resolver.allPairs();
		Set<ActionPair> unique = new HashSet<>();

		int actions = catalogue.definitions().size();
		assertEquals(actions * (actions + 1) / 2, pairs.size());
		for (ResolvedPair pair : pairs) {
			assertTrue(unique.add(pair.pair()), pair.pair().toString());
			assertNotNull(pair.resolution().outcome(), pair.pair().toString());
			assertTrue(pair.resolution().cue().isComplete(), pair.pair().toString());
			assertTrue(pair.resolution().hasFiniteMultipliers(), pair.pair().toString());
			assertTrue(!pair.resolution().mechanics().isBlank(), pair.pair().toString());
		}
	}

	@Test
	void flameAndFrostTransformIntoSteamInEitherOrder() {
		InteractionResolution forward = resolve("fireball", "ice_manipulation");
		InteractionResolution reverse = resolve("ice_manipulation", "fireball");

		assertEquals(InteractionOutcome.TRANSFORM, forward.outcome());
		assertEquals("steam", forward.cue().motif());
		assertEquals(InteractionOutcome.TRANSFORM, reverse.outcome());
		assertEquals("steam", reverse.cue().motif());
		assertEquals(forward.firstPotencyMultiplier(), reverse.secondPotencyMultiplier());
	}

	@Test
	void dimensionalAnchorCancelsTravelBeforeItCanCommit() {
		InteractionResolution resolution = resolve("time_shift", "dimensional_anchor");

		assertEquals(InteractionOutcome.CANCEL, resolution.outcome());
		assertTrue(resolution.blocksFirst());
		assertEquals("anchor_chains", resolution.cue().motif());
	}

	@Test
	void lightAndDarknessFormAnEclipseContest() {
		InteractionResolution resolution = resolve("light_crystal", "dark_crystal");

		assertEquals(InteractionOutcome.CONTEST, resolution.outcome());
		assertEquals("eclipse", resolution.cue().motif());
	}

	@Test
	void opposedRealmMatterMutuallyAnnihilates() {
		InteractionResolution resolution = resolve("darkness_block", "pure_light_block");

		assertEquals(InteractionOutcome.CANCEL, resolution.outcome());
		assertTrue(resolution.blocksFirst());
		assertTrue(resolution.blocksSecond());
		assertEquals("realm_annihilation", resolution.cue().motif());
	}

	@Test
	void sameAspectActionsResonateWhileUnrelatedActionsSafelyCoexist() {
		assertEquals(InteractionOutcome.RESONATE, resolve("fireball", "inferno").outcome());
		assertEquals(InteractionOutcome.COEXIST, resolve("flight", "soul_compass").outcome());
	}

	@Test
	void poweredAmethystWardOvercomesCrystalPriorityWithAFractureCue() {
		InteractionResolution resolution = resolve("chrono_stop", "amethyst_ward");

		assertEquals(InteractionOutcome.CANCEL, resolution.outcome());
		assertTrue(resolution.blocksFirst());
		assertEquals("amethyst_fracture", resolution.cue().motif());
	}

	private static InteractionResolution resolve(String first, String second) {
		return resolver.resolve(catalogue.definition(new MagicActionId(first)),
				catalogue.definition(new MagicActionId(second)), InteractionContext.DEFAULT);
	}
}
