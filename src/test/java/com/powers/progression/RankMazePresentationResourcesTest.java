package com.powers.progression;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Keeps the B-key progression screen distinct from the in-realm labyrinth landmark. */
class RankMazePresentationResourcesTest {
	@Test
	void screenUsesTheConciseApprovedHeading() throws Exception {
		Path languagePath = Path.of("src/main/resources/assets/powers/lang/en_us.json");
		JsonObject language = JsonParser.parseString(Files.readString(languagePath)).getAsJsonObject();

		assertEquals("Rank Maze", language.get("screen.powers.rank.title").getAsString());
		assertEquals("The Labyrinth of Names",
				language.get("realm.powers.landmark.labyrinth").getAsString());
	}
}
