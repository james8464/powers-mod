package com.powers.companion;

/** Pure physical and capacity limits shared by persistence, entity, and AI layers. */
public final class ShadowCompanionRules {
	public static final int MAX_ENERGY = 1_850;
	private static final double FOLLOW_DISTANCE_SQUARED = 4.0 * 4.0;
	private static final double TELEPORT_DISTANCE_SQUARED = 12.0 * 12.0;

	public record Presentation(boolean globallyVisible, boolean collidable,
			boolean externallyVulnerable) {
	}

	private ShadowCompanionRules() {
	}

	public static int energy(int requested) {
		return Math.clamp(requested, 0, MAX_ENERGY);
	}

	public static int recallEnergy() {
		return (int) Math.ceil(MAX_ENERGY * 0.25);
	}

	public static boolean shouldFollow(double distanceSquared) {
		return Double.isFinite(distanceSquared) && distanceSquared > FOLLOW_DISTANCE_SQUARED;
	}

	public static boolean shouldTeleport(double distanceSquared) {
		return Double.isFinite(distanceSquared) && distanceSquared > TELEPORT_DISTANCE_SQUARED;
	}

	public static Presentation presentation(boolean revealed) {
		return new Presentation(revealed, revealed, revealed);
	}
}
