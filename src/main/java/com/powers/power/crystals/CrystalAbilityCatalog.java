package com.powers.power.crystals;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Data-only crystal-to-ability plan, kept testable without Minecraft registries. */
public final class CrystalAbilityCatalog {
	private CrystalAbilityCatalog() {
	}

	public static Map<String, List<String>> defaults() {
		Map<String, List<String>> bindings = new LinkedHashMap<>();
		bindings.put("red_crystal", List.of("inferno"));
		bindings.put("orange_crystal", List.of("clone_swarm", "creativity_manifestation"));
		bindings.put("yellow_crystal", List.of("size_shift"));
		bindings.put("green_crystal", List.of("life_bloom", "space_time"));
		bindings.put("blue_crystal", List.of("chrono_stop", "dreamwalking"));
		bindings.put("indigo_crystal", List.of("portal_rift", "middleworld"));
		bindings.put("violet_crystal", List.of("soul_link"));
		bindings.put("rainbow_crystal", List.of("inferno", "clone_swarm", "size_shift",
				"life_bloom", "chrono_stop", "portal_rift", "soul_link"));
		bindings.put("infected_rainbow_crystal", List.of("inferno", "chrono_stop", "portal_rift", "soul_link"));
		bindings.put("light_crystal", List.of("light_crystal"));
		bindings.put("dark_crystal", List.of("dark_crystal"));
		return Map.copyOf(bindings);
	}
}
