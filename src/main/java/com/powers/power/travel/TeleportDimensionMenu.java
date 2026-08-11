package com.powers.power.travel;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Pure ordering and visibility policy for server-advertised teleport dimensions. */
public final class TeleportDimensionMenu {
	private static final List<String> VANILLA_FIRST = List.of(
			"minecraft:overworld", "minecraft:the_nether", "minecraft:the_end");

	private TeleportDimensionMenu() {
	}

	public static List<String> visibleIds(Collection<String> serverIds, boolean showDarkRealm) {
		Set<String> unique = new LinkedHashSet<>();
		for (String id : serverIds) {
			if (id != null && !id.isBlank() && !id.equals("powers:middleworld")
					&& (showDarkRealm || !id.equals("powers:dark_realm"))) {
				unique.add(id);
			}
		}
		return unique.stream().sorted(Comparator
				.comparingInt((String id) -> {
					int vanilla = VANILLA_FIRST.indexOf(id);
					return vanilla < 0 ? VANILLA_FIRST.size() : vanilla;
				})
				.thenComparing(Comparator.naturalOrder())).toList();
	}
}
