package com.powers.spell;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Complete spell catalogue plus aliases used by the imported recolour textures. */
public final class SpellRegistry {
	private final List<GrimoireDefinition> definitions;
	private final Map<String, GrimoireDefinition> byTexture;

	private SpellRegistry(List<GrimoireDefinition> definitions) {
		this.definitions = List.copyOf(definitions);
		Map<String, GrimoireDefinition> indexed = new LinkedHashMap<>();
		Map<String, SpellDefinition> unique = new LinkedHashMap<>();
		for (GrimoireDefinition definition : definitions) {
			indexed.put(definition.key(), definition);
			for (SpellDefinition spell : definition.spells()) {
				if (unique.putIfAbsent(spell.id(), spell) != null) {
					throw new IllegalArgumentException("Duplicate spell id: " + spell.id());
				}
			}
		}
		alias(indexed, "book_grimoire_recolor", "book_grimoire_abyssal");
		for (String school : List.of("abyssal", "blight", "celestial", "deep", "infernal", "wild")) {
			alias(indexed, "book_grimoire_recolor_overlay_" + school, "book_grimoire_" + school);
		}
		this.byTexture = Map.copyOf(indexed);
	}

	public static SpellRegistry defaults() {
		return new SpellRegistry(List.of(
				book("celestial",
						spell("soul_compass", 14, 200, 0, SpellEffect.SOUL_COMPASS),
						spell("tracking_mark", 18, 500, 40, SpellEffect.TRACKING_MARK),
						spell("weather_sigil", 22, 1200, 80, SpellEffect.WEATHER_SIGIL)),
				book("deep",
						spell("dimensional_anchor", 22, 1200, 40, SpellEffect.DIMENSIONAL_ANCHOR),
						spell("binding_sigil", 16, 400, 30, SpellEffect.BINDING_SIGIL),
						spell("anti_portal_field", 24, 1000, 60, SpellEffect.ANTI_PORTAL_FIELD),
						spell("kinetic_ward", 18, 600, 20, SpellEffect.KINETIC_WARD)),
				book("blight",
						spell("vitality_transfer", 18, 500, 30, SpellEffect.VITALITY_TRANSFER),
						spell("hex", 20, 800, 40, SpellEffect.HEX),
						spell("concealment_veil", 16, 600, 20, SpellEffect.CONCEALMENT_VEIL)),
				book("wild",
						spell("purification_circle", 20, 600, 50, SpellEffect.PURIFICATION_CIRCLE),
						spell("root_binding", 16, 400, 30, SpellEffect.ROOT_BINDING),
						spell("sanctuary_growth", 24, 1000, 60, SpellEffect.SANCTUARY_GROWTH)),
				book("infernal",
						spell("infernal_seal", 20, 800, 40, SpellEffect.INFERNAL_SEAL),
						spell("banishment_circle", 24, 1000, 60, SpellEffect.BANISHMENT_CIRCLE),
						spell("controlled_hellfire", 18, 500, 20, SpellEffect.CONTROLLED_HELLFIRE)),
				book("abyssal",
						spell("ward_breaking_ritual", 26, 1200, 80, SpellEffect.WARD_BREAKING_RITUAL),
						spell("counterspell", 16, 300, 0, SpellEffect.COUNTERSPELL),
						spell("dispel", 18, 500, 20, SpellEffect.DISPEL),
						spell("ritual_amplification", 22, 900, 50, SpellEffect.RITUAL_AMPLIFICATION))));
	}

	private static GrimoireDefinition book(String school, SpellDefinition... spells) {
		return new GrimoireDefinition("book_grimoire_" + school, List.of(spells));
	}

	private static SpellDefinition spell(String id, int energy, int cooldown, int channel, SpellEffect effect) {
		return new SpellDefinition(id, energy, cooldown, channel, 0, effect);
	}

	private static void alias(Map<String, GrimoireDefinition> map, String alias, String canonical) {
		map.put(alias, map.get(canonical));
	}

	public GrimoireDefinition forTexture(String texture) {
		return byTexture.get(texture);
	}

	public Collection<GrimoireDefinition> definitions() {
		return definitions;
	}
}
