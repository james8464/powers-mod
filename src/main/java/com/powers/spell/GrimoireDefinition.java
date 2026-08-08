package com.powers.spell;

import java.util.List;

/** Immutable ordered spellbook definition keyed to a grimoire texture identity. */
public record GrimoireDefinition(String key, List<SpellDefinition> spells) {
	public GrimoireDefinition {
		if (key == null || key.isBlank()) throw new IllegalArgumentException("Grimoire key is required");
		spells = List.copyOf(spells);
		if (spells.isEmpty()) throw new IllegalArgumentException("A grimoire must contain a spell: " + key);
	}
}
