package com.powers.allegiance;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static com.powers.allegiance.AllegianceMatrix.Faction.DARKNESS;
import static com.powers.allegiance.AllegianceMatrix.Faction.RADIANT;
import static com.powers.allegiance.AllegianceMatrix.Relationship.ALLY;
import static com.powers.allegiance.AllegianceMatrix.Relationship.HOSTILE;
import static com.powers.allegiance.AllegianceMatrix.Role.BOSS;
import static com.powers.allegiance.AllegianceMatrix.Role.PLAYER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AllegianceRegistryTest {
	@Test
	void reloadPublishesOneCompleteRevisionAndRejectsPartialReplacementAtomically() {
		AllegianceRegistry registry = new AllegianceRegistry();
		assertEquals(0L, registry.snapshot().revision());

		assertTrue(registry.reload(JsonParser.parseString("""
				{"same":"hostile","opposed":"ally","neutral":"neutrality",
				 "party":"ally","amethyst":"suppressed"}
				""").getAsJsonObject()));
		assertEquals(1L, registry.snapshot().revision());
		assertEquals(ALLY, registry.snapshot().matrix().resolve(
				new AllegianceMatrix.Profile(RADIANT, PLAYER),
				new AllegianceMatrix.Profile(DARKNESS, BOSS), false));

		assertFalse(registry.reload(JsonParser.parseString("{\"same\":\"ally\"}").getAsJsonObject()));
		assertEquals(1L, registry.snapshot().revision());
		assertEquals(HOSTILE, registry.snapshot().matrix().resolve(
				new AllegianceMatrix.Profile(DARKNESS, PLAYER),
				new AllegianceMatrix.Profile(DARKNESS, BOSS), false));
	}
}
