package com.powers.allegiance;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static com.powers.allegiance.AllegianceMatrix.Faction.DARKNESS;
import static com.powers.allegiance.AllegianceMatrix.Faction.NEUTRAL;
import static com.powers.allegiance.AllegianceMatrix.Faction.RADIANT;
import static com.powers.allegiance.AllegianceMatrix.Relationship.ALLY;
import static com.powers.allegiance.AllegianceMatrix.Relationship.HOSTILE;
import static com.powers.allegiance.AllegianceMatrix.Relationship.NEUTRALITY;
import static com.powers.allegiance.AllegianceMatrix.Relationship.SUPPRESSED;
import static com.powers.allegiance.AllegianceMatrix.Role.AMETHYST;
import static com.powers.allegiance.AllegianceMatrix.Role.ARTIFACT;
import static com.powers.allegiance.AllegianceMatrix.Role.BOSS;
import static com.powers.allegiance.AllegianceMatrix.Role.FOOD;
import static com.powers.allegiance.AllegianceMatrix.Role.FORCE;
import static com.powers.allegiance.AllegianceMatrix.Role.PARTY;
import static com.powers.allegiance.AllegianceMatrix.Role.PLAYER;
import static com.powers.allegiance.AllegianceMatrix.Role.SHADOW;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AllegianceMatrixTest {
	@Test
	void oneMatrixResolvesEveryAuthoredAllegianceRole() {
		AllegianceMatrix matrix = AllegianceMatrix.defaults();
		for (AllegianceMatrix.Role role : new AllegianceMatrix.Role[] {
				PLAYER, SHADOW, ARTIFACT, FORCE, BOSS, FOOD, PARTY}) {
			assertEquals(ALLY, matrix.resolve(profile(RADIANT, PLAYER), profile(RADIANT, role), false));
			assertEquals(HOSTILE, matrix.resolve(profile(RADIANT, PLAYER), profile(DARKNESS, role), false));
			assertEquals(NEUTRALITY, matrix.resolve(profile(RADIANT, PLAYER), profile(NEUTRAL, role), false));
		}
	}

	@Test
	void partyMembershipOverridesFactionOppositionButNotAmethystSuppression() {
		AllegianceMatrix matrix = AllegianceMatrix.defaults();
		assertEquals(ALLY, matrix.resolve(profile(RADIANT, PLAYER), profile(DARKNESS, SHADOW), true));
		assertEquals(SUPPRESSED,
				matrix.resolve(profile(RADIANT, PLAYER), profile(NEUTRAL, AMETHYST), true));
		assertEquals(SUPPRESSED,
				matrix.resolve(profile(DARKNESS, BOSS), profile(NEUTRAL, AMETHYST), false));
	}

	@Test
	void datapackRowsReplaceThePublishedSnapshotOnlyWhenComplete() {
		String complete = """
				{"same":"ally","opposed":"hostile","neutral":"neutrality",
				 "party":"ally","amethyst":"suppressed"}
				""";
		AllegianceMatrix parsed = AllegianceMatrix.parse(JsonParser.parseString(complete).getAsJsonObject());
		assertEquals(ALLY, parsed.resolve(profile(DARKNESS, ARTIFACT), profile(DARKNESS, FORCE), false));
		assertEquals(HOSTILE, parsed.resolve(profile(DARKNESS, BOSS), profile(RADIANT, PLAYER), false));

		String incomplete = "{\"same\":\"ally\"}";
		assertThrows(IllegalArgumentException.class,
				() -> AllegianceMatrix.parse(JsonParser.parseString(incomplete).getAsJsonObject()));
	}

	private static AllegianceMatrix.Profile profile(AllegianceMatrix.Faction faction,
			AllegianceMatrix.Role role) {
		return new AllegianceMatrix.Profile(faction, role);
	}
}
