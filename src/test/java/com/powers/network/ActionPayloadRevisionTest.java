package com.powers.network;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ActionPayloadRevisionTest {
	@Test
	void everyActionSelectionCarriesRegistryRevisionAndCanonicalKey() {
		var artifact = new ShadowSwordPackets.SelectPayload(7L, "darkness", "innate/fireball", -1);
		var spell = new GrimoirePackets.SelectSpellPayload(7L,
				"book_grimoire_celestial", "fireball");
		var crystal = new CrystalSelectorPackets.SelectPayload(7L, "fireball");

		assertEquals(7L, artifact.revision());
		assertEquals("innate/fireball", artifact.actionKey());
		assertEquals(7L, spell.revision());
		assertEquals("fireball", spell.spellId());
		assertEquals(7L, crystal.revision());
		assertEquals("fireball", crystal.actionKey());
	}
}
