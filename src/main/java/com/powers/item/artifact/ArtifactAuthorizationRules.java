package com.powers.item.artifact;

/** Pure authorization invariants shared by packet, item, aura, and menu paths. */
public final class ArtifactAuthorizationRules {
	private ArtifactAuthorizationRules() {
	}

	/** Darkness is exclusive to infected wielders; light is exclusive to the normal path. */
	public static boolean mayUse(ArtifactAlignment alignment, boolean darknessTagged) {
		return alignment == ArtifactAlignment.DARKNESS ? darknessTagged : !darknessTagged;
	}

	/** Ascendance alters cooldowns and costs but never creates free invocations. */
	public static boolean requiresEnergy(ArtifactAlignment alignment, int rank) {
		return alignment != null && rank >= 0;
	}
}
