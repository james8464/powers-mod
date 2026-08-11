package com.powers.spell;

import org.junit.jupiter.api.Test;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Ensures each active ritual advertises enough in-game information to use and counter it. */
class SpellIndexEntryTest {
	@Test
	void everyActiveSpellHasCompleteCompactIndexMetadata() {
		SpellRegistry registry = SpellRegistry.defaults();
		var ids = new HashSet<String>();
		for (GrimoireDefinition book : registry.definitions()) {
			for (SpellDefinition spell : book.spells()) {
				SpellIndexEntry entry = SpellIndexEntry.from(spell);
				assertTrue(ids.add(entry.id()), entry.id());
				assertEquals(spell.energyCost(), entry.energy());
				assertEquals(spell.cooldownTicks(), entry.cooldownTicks());
				assertEquals(spell.channelTicks(), entry.channelTicks());
				assertTrue(entry.range() >= 0 && Double.isFinite(entry.range()), entry.id());
				assertTrue(entry.purposeKey().startsWith("spell.powers.index.purpose."), entry.id());
				assertTrue(entry.targetKey().startsWith("spell.powers.index.target."), entry.id());
				assertTrue(entry.counterKey().startsWith("spell.powers.index.counter."), entry.id());
			}
		}
		assertEquals(12, ids.size());
	}
}
