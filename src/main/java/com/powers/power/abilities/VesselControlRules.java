package com.powers.power.abilities;

/** Pure validation and movement bounds for authenticated vessel-control packets. */
public final class VesselControlRules {
	public static final double MAX_HORIZONTAL_STEP = 0.35;
	public static final double VERTICAL_STEP = 0.30;
	private static final double MAX_ATTACK_DISTANCE_SQUARED = 36.0;

	public record Movement(double x, double y, double z) {
	}

	private VesselControlRules() {
	}

	public static Movement movement(float yaw, float forward, float strafe,
			boolean jump, boolean crouch) {
		double safeForward = Float.isFinite(forward) ? Math.clamp(forward, -1.0F, 1.0F) : 0.0;
		double safeStrafe = Float.isFinite(strafe) ? Math.clamp(strafe, -1.0F, 1.0F) : 0.0;
		double length = Math.hypot(safeForward, safeStrafe);
		if (length > 1.0) {
			safeForward /= length;
			safeStrafe /= length;
		}
		double radians = Math.toRadians(Float.isFinite(yaw) ? yaw : 0.0F);
		double x = (-Math.sin(radians) * safeForward + Math.cos(radians) * safeStrafe)
				* MAX_HORIZONTAL_STEP;
		double z = (Math.cos(radians) * safeForward + Math.sin(radians) * safeStrafe)
				* MAX_HORIZONTAL_STEP;
		double y = jump == crouch ? 0.0 : jump ? VERTICAL_STEP : -VERTICAL_STEP;
		return new Movement(x, y, z);
	}

	public static int hotbarSlot(int requested) {
		return Math.clamp(requested, 0, 8);
	}

	public static boolean mayAttack(double distanceSquared, boolean alive, boolean self) {
		return alive && !self && Double.isFinite(distanceSquared) && distanceSquared >= 0.0
				&& distanceSquared <= MAX_ATTACK_DISTANCE_SQUARED;
	}
}
