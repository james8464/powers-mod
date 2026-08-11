package com.powers.item.artifact;

/** Pure authorization invariants shared by packet, item, aura, and menu paths. */
public final class ArtifactAuthorizationRules {
	private ArtifactAuthorizationRules() {
	}

	/** Darkness is exclusive to infected wielders; light is exclusive to the normal path. */
	public static boolean mayUse(ArtifactAlignment alignment, boolean darknessTagged) {
		return alignment != null
				&& (alignment == ArtifactAlignment.DARKNESS ? darknessTagged : !darknessTagged);
	}

	/** Ascendance alters cooldowns and costs but never creates free invocations. */
	public static boolean requiresEnergy(ArtifactAlignment alignment, int rank) {
		return alignment != null && rank >= 0;
	}

	/** A channelled artifact effect ends as soon as any live ownership fact fails. */
	public static boolean maySustain(boolean magicAllowed, boolean held, boolean authorized) {
		return magicAllowed && held && authorized;
	}

	/** Inventory-owned effects require the artifact to remain carried and aligned. */
	public static boolean mayOwn(boolean carried, boolean authorized) {
		return carried && authorized;
	}

	/** Central policy for both held channels and inventory-owned companions/wards. */
	public static boolean maySustain(boolean magicAllowed, boolean carried, boolean held,
			boolean authorized, boolean requiresHeld) {
		return magicAllowed && mayOwn(carried, authorized) && (!requiresHeld || held);
	}
}
