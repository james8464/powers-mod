package com.powers.spell;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpellRegistryTest {
	private static final Set<String> REGISTERED_GRIMOIRES = Set.of(
			"book_grimoire_abyssal", "book_grimoire_blight", "book_grimoire_celestial",
			"book_grimoire_deep", "book_grimoire_infernal", "book_grimoire_wild",
			"book_grimoire_recolor", "book_grimoire_recolor_overlay_abyssal",
			"book_grimoire_recolor_overlay_blight", "book_grimoire_recolor_overlay_celestial",
			"book_grimoire_recolor_overlay_deep", "book_grimoire_recolor_overlay_infernal",
			"book_grimoire_recolor_overlay_wild");

	@Test
	void activeGrimoiresResolveAndInfernalCompatibilityTexturesAreDormant() {
		SpellRegistry registry = SpellRegistry.defaults();
		for (String grimoire : REGISTERED_GRIMOIRES) {
			if (grimoire.contains("infernal")) {
				assertTrue(registry.isDormantTexture(grimoire), grimoire);
				continue;
			}
			GrimoireDefinition definition = registry.forTexture(grimoire);
			assertNotNull(definition, grimoire);
			assertFalse(definition.spells().isEmpty(), grimoire);
		}
	}

	@Test
	void spellIdsAreUniqueAndDefinitionsAreComplete() {
		SpellRegistry registry = SpellRegistry.defaults();
		Set<String> ids = new HashSet<>();
		int count = 0;
		for (GrimoireDefinition grimoire : registry.definitions()) {
			for (SpellDefinition spell : grimoire.spells()) {
				count++;
				assertTrue(ids.add(spell.id()), spell.id());
				assertTrue(spell.energyCost() > 0, spell.id());
				assertTrue(spell.cooldownTicks() > 0, spell.id());
				assertTrue(spell.channelTicks() >= 0, spell.id());
			}
		}
		assertEquals(12, count);
		assertTrue(ids.contains("celestial_ruin"));
		assertEquals(List.of("dimensional_anchor"), spellIds(
				registry.forTexture("book_grimoire_deep")));
		assertEquals(List.of("blood_reading", "grave_recall"), spellIds(
				registry.forTexture("book_grimoire_blight")));
		assertEquals(List.of("ward_breaking_ritual", "dispel"), spellIds(
				registry.forTexture("book_grimoire_abyssal")));
	}

	private static List<String> spellIds(GrimoireDefinition definition) {
		return definition.spells().stream().map(SpellDefinition::id).toList();
	}
}
