package com.powers.power.abilities;

/** Pure forward-cone geometry for the boss-scale Thunderclap shockwave. */
public final class ThunderclapRules {
	private static final double MINIMUM_DOT = Math.cos(Math.toRadians(70.0));
	private static final double MINIMUM_LENGTH_SQUARED = 1.0E-8;

	/** Unit horizontal direction independent of Minecraft's vector classes. */
	public record HorizontalDirection(double x, double z) {
	}

	private ThunderclapRules() {
	}

	/**
	 * Projects the look vector onto the ground. Looking exactly vertically has
	 * no projection, so the player's yaw supplies the stable forward direction.
	 */
	public static HorizontalDirection horizontalDirection(double lookX, double lookZ, float yawDegrees) {
		double lengthSquared = lookX * lookX + lookZ * lookZ;
		if (lengthSquared > MINIMUM_LENGTH_SQUARED) {
			double inverseLength = 1.0 / Math.sqrt(lengthSquared);
			return new HorizontalDirection(lookX * inverseLength, lookZ * inverseLength);
		}
		double yaw = Math.toRadians(yawDegrees);
		return new HorizontalDirection(-Math.sin(yaw), Math.cos(yaw));
	}

	public static boolean inCone(double offsetX, double offsetZ,
			double lookX, double lookZ, double range) {
		double distanceSquared = offsetX * offsetX + offsetZ * offsetZ;
		if (distanceSquared > range * range) return false;
		if (distanceSquared <= MINIMUM_LENGTH_SQUARED) return true;
		double lookLength = Math.sqrt(lookX * lookX + lookZ * lookZ);
		if (lookLength <= Math.sqrt(MINIMUM_LENGTH_SQUARED)) return false;
		double dot = (offsetX * lookX + offsetZ * lookZ)
				/ (Math.sqrt(distanceSquared) * lookLength);
		return dot >= MINIMUM_DOT;
	}
}
