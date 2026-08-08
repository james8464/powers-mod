package com.powers.hud;

public final class HudMath {
	private HudMath() {
	}

	public static int filledSegments(int energy, int capacity, int segments) {
		if (capacity <= 0 || segments <= 0) return 0;
		long clamped = Math.max(0, Math.min(capacity, energy));
		return (int) (clamped * segments / capacity);
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
}
