package com.powers.realm;

import net.minecraft.world.level.GameType;

/** One-shot compatibility policy for saves produced by the retired realm coercion. */
public final class LegacyRealmGamemodeRules {
	/** A nullable restoration and whether the legacy attachment must be erased. */
	public record Decision(GameType restore, boolean clearSnapshot) {
	}

	private LegacyRealmGamemodeRules() {
	}

	public static Decision decide(String storedMode, GameType currentMode) {
		boolean present = storedMode != null && !storedMode.isBlank();
		GameType stored = parse(storedMode);
		GameType restore = currentMode == GameType.ADVENTURE && stored != GameType.ADVENTURE
				? stored : null;
		return new Decision(restore, present);
	}

	private static GameType parse(String name) {
		if (name == null || name.isBlank()) return null;
		for (GameType mode : GameType.values()) {
			if (mode.getName().equals(name)) return mode;
		}
		return null;
	}
}
