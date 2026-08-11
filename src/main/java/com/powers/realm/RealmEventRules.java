package com.powers.realm;

/** Pure timing and pressure rules for deterministic mindscape events. */
public final class RealmEventRules {
	public static final long CYCLE_TICKS = 14_400L;
	public static final long EVENT_START_TICK = 12_000L;

	private RealmEventRules() {
	}

	public static RealmEventType eventAt(RealmKind kind, long gameTime) {
		long phase = Math.floorMod(gameTime, CYCLE_TICKS);
		if (phase < EVENT_START_TICK) return RealmEventType.NONE;
		return kind == RealmKind.DARK ? RealmEventType.DARK_ECLIPSE : RealmEventType.WHITEOUT;
	}

	public static int pressureTier(double distance, boolean eventActive) {
		int tier = distance >= 72.0 ? 3 : distance >= 48.0 ? 2 : distance >= 24.0 ? 1 : 0;
		return eventActive ? Math.min(3, tier + 1) : tier;
	}
}
