package com.powers.item;

/** Pure policy constants for the Shadow Sword's bounded inventory effects. */
public final class ShadowSwordRules {
	public static final int AUTHORIZED_REGEN_PER_SECOND = 50;
	public static final int MAX_PROTECTORS = 4;
	public static final int MAX_COMMANDED_GUARDIANS = 32;
	public static final int SPREAD_RADIUS = 6;

	private ShadowSwordRules() {
	}

	/** Darkness infection is the sword's only authorization credential. */
	public static boolean mayUse(boolean darknessTagged) {
		return darknessTagged;
	}

	/** Darkness rank ten turns the sword into a cooldown-free divine conduit. */
	public static boolean bypassesCooldown(int darknessLevel) {
		return darknessLevel >= 10;
	}

	/** Affinity rises sharply through the darkness ladder: 50..250 energy each second. */
	public static int regenerationPerSecond(int darknessLevel) {
		return AUTHORIZED_REGEN_PER_SECOND + Math.clamp(darknessLevel, 0, 10) * 20;
	}

	/** Summons no more than two guardians and never exceeds the local cap. */
	public static int protectorsToSummon(int nearbyProtectors) {
		return Math.max(0, Math.min(2, MAX_PROTECTORS - Math.max(0, nearbyProtectors)));
	}

	/** Caps deliberate summons even when rank-ten invocation has no cooldown. */
	public static int commandedGuardiansToSummon(int requested, int nearbyGuardians) {
		return Math.max(0, Math.min(Math.clamp(requested, 1, 8),
				MAX_COMMANDED_GUARDIANS - Math.max(0, nearbyGuardians)));
	}

	/** Circular footprint used by ground corruption, inclusive at the edge. */
	public static boolean inSpreadDisc(int offsetX, int offsetZ) {
		return offsetX * offsetX + offsetZ * offsetZ <= SPREAD_RADIUS * SPREAD_RADIUS;
	}
}
