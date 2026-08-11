package com.powers.power.crystals;

import java.util.ArrayList;
import java.util.List;

/** Fixed server-authored Orange Crystal structure; clients never submit blocks. */
public final class CreationChamberBlueprint {
	public enum Role { FRAME, GLASS, LIGHT }

	public record Offset(int x, int y, int z) {
	}

	public record Placement(Offset offset, Role role) {
	}

	private static final List<Placement> PLACEMENTS = build();

	private CreationChamberBlueprint() {
	}

	public static List<Placement> placements() {
		return PLACEMENTS;
	}

	private static List<Placement> build() {
		List<Placement> result = new ArrayList<>(26);
		for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) {
			if (Math.abs(x) == 2 || Math.abs(z) == 2) {
				result.add(new Placement(new Offset(x, 0, z), Role.FRAME));
			}
		}
		for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
			result.add(new Placement(new Offset(x, 1, z), Role.GLASS));
		}
		result.add(new Placement(new Offset(0, 2, 0), Role.LIGHT));
		return List.copyOf(result);
	}
}
