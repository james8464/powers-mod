package com.powers.spell;

import java.util.List;

public record GrimoireDefinition(String key, List<SpellDefinition> spells) {
	public GrimoireDefinition {
		if (key == null || key.isBlank()) throw new IllegalArgumentException("Grimoire key is required");
		spells = List.copyOf(spells);
		if (spells.isEmpty()) throw new IllegalArgumentException("A grimoire must contain a spell: " + key);
	}
}
