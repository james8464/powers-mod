package com.powers.realm;

import java.util.List;

/** Stable hexagonal landmark layout shared by world construction and tests. */
public final class RealmLayout {
	public static final double ENTRY_X = 8.5;
	public static final double ENTRY_Z = 8.5;
	public static final double TETHER_RADIUS = 88.0;

	private RealmLayout() {
	}

	public static List<MemorySite> sites(RealmKind kind) {
		String prefix = kind == RealmKind.LIGHT ? "light" : "dark";
		String[] paths = kind == RealmKind.LIGHT
				? new String[] {"might_1", "motion_1", "insight_1", "ward_2", "veil_2", "communion_2"}
				: new String[] {"fang_1", "mist_1", "echo_1", "hex_2", "shroud_2", "hollow_2"};
		int[][] points = {{36, 8}, {22, 32}, {-6, 32}, {-20, 8}, {-6, -16}, {22, -16}};
		java.util.ArrayList<MemorySite> sites = new java.util.ArrayList<>();
		for (int index = 0; index < points.length; index++) {
			sites.add(new MemorySite(prefix + "_memory_" + (index + 1),
					points[index][0], points[index][1], paths[index]));
		}
		return List.copyOf(sites);
	}
}
