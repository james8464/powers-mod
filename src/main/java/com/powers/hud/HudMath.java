package com.powers.hud;

import com.powers.power.abilities.ElementalPhase;

/** Pure clamped arithmetic used by energy HUD renderers and tests. */
public final class HudMath {
	private HudMath() {
	}

	public static int filledSegments(int energy, int capacity, int segments) {
		if (capacity <= 0 || segments <= 0) return 0;
		long clamped = Math.max(0, Math.min(capacity, energy));
		return (int) (clamped * segments / capacity);
	}

	/** Maps energy to the ten full/half symbols used by vanilla resource bars. */
	public static int energyHalfUnits(int energy, int capacity) {
		if (capacity <= 0 || energy <= 0) return 0;
		long clamped = Math.min(capacity, energy);
		return (int) Math.min(20, (clamped * 20L + capacity - 1L) / capacity);
	}

	public static HudEnergyMode mode(int energy, boolean dampened, boolean darkness) {
		return mode(energy, dampened, darkness, false);
	}

	public static HudEnergyMode mode(int energy, boolean dampened, boolean darkness, boolean projection) {
		if (dampened) return HudEnergyMode.DAMPENED;
		if (energy <= 0) return HudEnergyMode.EMPTY;
		if (projection) return HudEnergyMode.PROJECTION;
		return darkness ? HudEnergyMode.DARKNESS : HudEnergyMode.NORMAL;
	}

	public static int cooldownSegments(int remaining, int maximum, int segments) {
		if (remaining <= 0 || maximum <= 0 || segments <= 0) return 0;
		long clamped = Math.min(maximum, remaining);
		return (int) Math.min(segments, (clamped * segments + maximum - 1L) / maximum);
	}

	/** Returns a phase-coloured HUD rune with a bounded active pulse. */
	public static int elementalRuneColor(int currentPhase, int runePhase, int tick) {
		ElementalPhase current = ElementalPhase.fromIndex(currentPhase);
		ElementalPhase rune = ElementalPhase.fromIndex(runePhase);
		int alpha;
		if (current == rune) {
			alpha = (Math.floorDiv(tick, 5) & 1) == 0 ? 0xFF000000 : 0xCC000000;
		} else {
			alpha = 0x55000000;
		}
		return alpha | rune.color();
	}

	/** Alternates cyan and gold runes to show one legal ranked follow-up cast. */
	public static int secondStepRuneColor(int rune, int tick) {
		int rgb = (rune & 1) == 0 ? 0xD7F8FF : 0xFFD166;
		int pulse = Math.floorDiv(Math.max(0, tick), 4);
		int alpha = ((pulse + rune) & 1) == 0 ? 0xFF000000 : 0xCC000000;
		return alpha | rgb;
	}
}
