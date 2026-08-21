package com.powers.force;

import java.util.ArrayList;
import java.util.List;

/** Pure allegiance and hard workload caps for living-force incursions. */
public final class FactionInvasionRules {
	public static final int GLOBAL_INVADER_CAP = 64;
	public static final int NEARBY_INVADER_CAP = 3;
	public static final int INVADER_LIFETIME_TICKS = 2_400;
	public static final int PLAYER_ANCHOR_CAP = 64;
	private static final List<Offset> SCAR_OFFSETS = List.of(
			new Offset(0, 0), new Offset(1, 0), new Offset(-1, 0),
			new Offset(0, 1), new Offset(0, -1));

	private FactionInvasionRules() {
	}

	public static boolean shouldInvade(LivingForceKind kind, boolean darknessTagged) {
		return kind == LivingForceKind.DARKNESS ? !darknessTagged : darknessTagged;
	}

	public static List<Offset> scarOffsets() {
		return SCAR_OFFSETS;
	}

	public static int initialPlayerAnchorCursor() {
		return 0;
	}

	public static AnchorWindow playerAnchorWindow(int playerCount, int cursor) {
		if (playerCount <= 0) return new AnchorWindow(List.of(), initialPlayerAnchorCursor());
		int start = Math.floorMod(cursor, playerCount);
		int size = Math.min(PLAYER_ANCHOR_CAP, playerCount);
		List<Integer> indexes = new ArrayList<>(size);
		for (int offset = 0; offset < size; offset++) {
			indexes.add((start + offset) % playerCount);
		}
		return new AnchorWindow(List.copyOf(indexes), (start + size) % playerCount);
	}

	public record AnchorWindow(List<Integer> indexes, int nextCursor) {
	}

	public record Offset(int x, int z) {
	}
}
