package com.powers.spell;

import com.google.gson.JsonParser;
import com.powers.force.LoadedFrontForecast;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuguryPresentationTest {
	@Test
	void truncatedEmptySampleUsesTheIncompleteFrontTranslation() {
		LoadedFrontForecast incomplete = new LoadedFrontForecast(0, -1, true);

		assertEquals("spell.powers.augury.front.incomplete",
				SpellEffects.frontTranslationKey(incomplete));
	}

	@Test
	void incompleteForecastTranslationsAreRegistered() throws Exception {
		var language = JsonParser.parseString(Files.readString(Path.of(
				"src/main/resources/assets/powers/lang/en_us.json"))).getAsJsonObject();

		assertTrue(language.has("spell.powers.augury.front.incomplete"));
		assertTrue(language.has("spell.powers.augury.ritual.indeterminate"));
	}
}
