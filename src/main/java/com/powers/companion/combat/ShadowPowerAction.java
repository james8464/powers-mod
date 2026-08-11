package com.powers.companion.combat;

/** Immutable combat metadata used by both the planner and the executor. */
public record ShadowPowerAction(String id, RangeMode range, Intent intent,
		int destructionTier, boolean toggle, WorkClass workClass, int cost) {
	public enum RangeMode { SELF, CLOSE, MID, FAR, FLEXIBLE }
	public enum Intent { OFFENSE, DEFENSE, MOBILITY, CONTROL, RECOVERY, SUMMON, TERRAIN }
	public enum WorkClass { CHEAP, ENTITY_QUERY, PROJECTILE, TERRAIN, GLOBAL }

	public ShadowPowerAction {
		if (id == null || id.isBlank()) throw new IllegalArgumentException("Missing Shadow action ID");
		range = range == null ? RangeMode.FLEXIBLE : range;
		intent = intent == null ? Intent.OFFENSE : intent;
		destructionTier = Math.clamp(destructionTier, 0, 10);
		workClass = workClass == null ? WorkClass.CHEAP : workClass;
		cost = Math.max(0, cost);
	}
}
