package com.powers.spell;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Complete spell catalogue plus aliases used by the imported recolour textures. */
public final class SpellRegistry {
	private final List<GrimoireDefinition> definitions;
	private final Map<String, GrimoireDefinition> byTexture;
	private final Set<String> dormantTextures;

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
		for (String school : List.of("abyssal", "blight", "celestial", "deep", "wild")) {
			alias(indexed, "book_grimoire_recolor_overlay_" + school, "book_grimoire_" + school);
		}
		this.byTexture = Map.copyOf(indexed);
		this.dormantTextures = Set.of("book_grimoire_infernal",
				"book_grimoire_recolor_overlay_infernal");
	}

	public static SpellRegistry defaults() {
		return new SpellRegistry(List.of(
				book("celestial",
						spell("soul_compass", 14, 200, 0, SpellEffect.SOUL_COMPASS),
						spell("augury", 16, 600, 20, SpellEffect.AUGURY),
						spell("cartographers_star", 24, 1200, 0, SpellEffect.CARTOGRAPHERS_STAR),
						spell("celestial_ruin", 100, 72_000, 200, SpellEffect.CELESTIAL_RUIN)),
				book("deep",
						spell("dimensional_anchor", 22, 1200, 40, SpellEffect.DIMENSIONAL_ANCHOR)),
				book("blight",
						spell("blood_reading", 12, 200, 20, SpellEffect.BLOOD_READING),
						spell("grave_recall", 10, 200, 0, SpellEffect.GRAVE_RECALL)),
				book("wild",
						spell("purification_circle", 20, 600, 50, SpellEffect.PURIFICATION_CIRCLE),
						spell("verdant_tending", 22, 600, 40, SpellEffect.VERDANT_TENDING),
						spell("hearth_sanctuary", 28, 1000, 40, SpellEffect.HEARTH_SANCTUARY)),
				book("abyssal",
						spell("ward_breaking_ritual", 26, 1200, 80, SpellEffect.WARD_BREAKING_RITUAL),
						spell("dispel", 18, 500, 20, SpellEffect.DISPEL))));
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

	/** Historical Infernal item textures remain registered but expose no active spells. */
	public boolean isDormantTexture(String texture) {
		return dormantTextures.contains(texture);
	}

	public Collection<GrimoireDefinition> definitions() {
		return definitions;
	}
}
