package com.powers.magic.fx;

import com.powers.magic.MagicActionCatalogue;
import com.powers.magic.MagicActionDefinition;
import com.powers.magic.MagicActionId;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Guards the catalogue-to-presentation boundary used by every successful cast. */
class MagicCastPresentationTest {
	private static final Set<String> AUTHORED_CUES = Set.of(
			"rune_hum", "crystal_resonate", "amethyst_fracture", "time_suspend",
			"rift_open", "soul_tether", "light_chorus", "dark_whisper", "ward_impact");

	@Test
	void everyCatalogueActionResolvesToABoundedAuthoredProfile() {
		for (MagicActionDefinition action : MagicActionCatalogue.defaults().definitions()) {
			MagicCastPresentation presentation = MagicCastPresentation.forAction(action);
			assertTrue(AUTHORED_CUES.contains(presentation.soundCue()), action.id().toString());
			assertTrue(presentation.intensity() >= 1 && presentation.intensity() <= 5,
					action.id().toString());
		}
	}

	@Test
	void significanceControlsGenericCeremonyWithoutDuplicatingBespokeEffects() {
		MagicActionCatalogue catalogue = MagicActionCatalogue.defaults();

		assertEquals(0, profile(catalogue, "darkness_block").genericBeatCount());
		assertEquals(0, profile(catalogue, "lightning_strike").genericBeatCount());
		assertEquals(0, profile(catalogue, "fireball").genericBeatCount());
		assertEquals(2, profile(catalogue, "telekinesis").genericBeatCount());
		assertEquals(4, profile(catalogue, "dimensional_anchor").genericBeatCount());
		assertEquals(6, profile(catalogue, "time_freeze").genericBeatCount());
		assertEquals(6, profile(catalogue, "celestial_ruin").genericBeatCount());
	}

	@Test
	void mechanicalAspectsChooseDistinctSoundLanguages() {
		MagicActionCatalogue catalogue = MagicActionCatalogue.defaults();

		assertEquals("time_suspend", profile(catalogue, "time_freeze").soundCue());
		assertEquals("rift_open", profile(catalogue, "middleworld").soundCue());
		assertEquals("soul_tether", profile(catalogue, "astral_projection").soundCue());
		assertEquals("light_chorus", profile(catalogue, "starfall").soundCue());
		assertEquals("dark_whisper", profile(catalogue, "void_beam").soundCue());
		assertEquals("ward_impact", profile(catalogue, "forcefield").soundCue());
		assertEquals("amethyst_fracture", profile(catalogue, "amethyst_block").soundCue());
		assertEquals("crystal_resonate", profile(catalogue, "size_shift").soundCue());
		assertEquals("rune_hum", profile(catalogue, "plant_healing_acceleration").soundCue());
	}

	@Test
	void strongerOriginsAndFieldsReadMoreIntenselyThanUtilityToggles() {
		MagicActionCatalogue catalogue = MagicActionCatalogue.defaults();

		int flight = profile(catalogue, "flight").intensity();
		int spaceTime = profile(catalogue, "chrono_stop").intensity();
		int realmForce = profile(catalogue, "darkness_block").intensity();
		assertTrue(flight < spaceTime);
		assertTrue(spaceTime < realmForce);
	}

	@Test
	void legacyDepthCrossesTwoVisibleMasteryThresholds() {
		MagicActionDefinition flight = MagicActionCatalogue.defaults()
				.definition(new MagicActionId("flight"));

		assertEquals(1, MagicCastPresentation.forAction(flight, 3, Set.of()).intensity());
		assertEquals(2, MagicCastPresentation.forAction(flight, 4, Set.of()).intensity());
		assertEquals(3, MagicCastPresentation.forAction(flight, 8, Set.of()).intensity());
	}

	@Test
	void ancientMasteryAddsAThirdBoundedCeremonyStep() {
		MagicActionDefinition flight = MagicActionCatalogue.defaults()
				.definition(new MagicActionId("flight"));

		assertEquals(4, MagicCastPresentation.forAction(
				flight, 8, Set.of("ancient_mastery")).intensity());
		assertEquals(1, MagicCastPresentation.forAction(flight, -50, Set.of()).intensity());
		assertTrue(MagicCastPresentation.forAction(
				flight, 500, Set.of("ancient_mastery")).intensity() <= 5);
		assertThrows(NullPointerException.class,
				() -> MagicCastPresentation.forAction(flight, 4, null));
	}

	@Test
	void everyRankedCatalogueProfileRemainsInsideTheClientContract() {
		for (MagicActionDefinition action : MagicActionCatalogue.defaults().definitions()) {
			for (int level : new int[] {-1, 0, 4, 8, 10, 100}) {
				int intensity = MagicCastPresentation.forAction(
						action, level, Set.of("ancient_mastery")).intensity();
				assertTrue(intensity >= 1 && intensity <= 5, action.id() + "@" + level);
			}
		}
	}

	private static MagicCastPresentation profile(MagicActionCatalogue catalogue, String id) {
		return MagicCastPresentation.forAction(catalogue.definition(new MagicActionId(id)));
	}
}
