package com.powers.testing;

import java.util.List;

/** Small loaded-chunk target arrangement used by the operator's manual test arena. */
public final class TestingArenaLayout {
	public enum TargetKind {
		NEUTRAL_ACTOR, RADIANT_ACTOR, DARKNESS_ACTOR,
		ZOMBIE, IRON_GOLEM, DARKNESS_CREATURE, RADIANT_SENTINEL
	}

	public record Target(TargetKind kind, String name, int x, int z) {
	}

	private static final List<Target> TARGETS = List.of(
			new Target(TargetKind.NEUTRAL_ACTOR, "ArenaNeutral", -6, 4),
			new Target(TargetKind.RADIANT_ACTOR, "ArenaRadiant", 0, 6),
			new Target(TargetKind.DARKNESS_ACTOR, "ArenaDarkness", 6, 4),
			new Target(TargetKind.ZOMBIE, "ArenaZombie", -6, -4),
			new Target(TargetKind.IRON_GOLEM, "ArenaGolem", 0, -6),
			new Target(TargetKind.DARKNESS_CREATURE, "ArenaHollowed", 6, -4),
			new Target(TargetKind.RADIANT_SENTINEL, "ArenaSentinel", 0, 3));

	private TestingArenaLayout() {
	}

	public static List<Target> targets() {
		return TARGETS;
	}
}
