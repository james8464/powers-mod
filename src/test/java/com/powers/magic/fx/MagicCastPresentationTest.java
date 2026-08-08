package com.powers.magic.fx;

import com.powers.magic.MagicActionCatalogue;
import com.powers.magic.MagicActionDefinition;
import com.powers.magic.MagicActionId;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
	void mechanicalAspectsChooseDistinctSoundLanguages() {
		MagicActionCatalogue catalogue = MagicActionCatalogue.defaults();

		assertEquals("time_suspend", profile(catalogue, "time_freeze").soundCue());
		assertEquals("rift_open", profile(catalogue, "portal_rift").soundCue());
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
		int spaceTime = profile(catalogue, "space_time").intensity();
		int realmForce = profile(catalogue, "darkness_block").intensity();
		assertTrue(flight < spaceTime);
		assertTrue(spaceTime < realmForce);
	}

	private static MagicCastPresentation profile(MagicActionCatalogue catalogue, String id) {
		return MagicCastPresentation.forAction(catalogue.definition(new MagicActionId(id)));
	}
}
