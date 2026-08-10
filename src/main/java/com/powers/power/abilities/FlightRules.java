package com.powers.power.abilities;

/** Pure propulsion calculation for survival flight; no creative-flight flags are involved. */
public final class FlightRules {
	public record Motion(double x, double y, double z) {
		public double horizontalSpeed() {
			return Math.sqrt(x * x + z * z);
		}
	}

	private FlightRules() {
	}

	public static Motion motion(float yawDegrees, boolean forward, boolean backward,
			boolean left, boolean right, boolean jump, boolean shift, boolean sprint, int level) {
		double yaw = Math.toRadians(yawDegrees);
		double forwardX = -Math.sin(yaw);
		double forwardZ = Math.cos(yaw);
		double sideX = -forwardZ;
		double sideZ = forwardX;
		double longitudinal = (forward ? 1.0 : 0.0) - (backward ? 1.0 : 0.0);
		double lateral = (right ? 1.0 : 0.0) - (left ? 1.0 : 0.0);
		double x = forwardX * longitudinal + sideX * lateral;
		double z = forwardZ * longitudinal + sideZ * lateral;
		double length = Math.sqrt(x * x + z * z);
		double speed = (sprint ? 3.2 : 1.15) * (1.0 + Math.clamp(level, 0, 10) * 0.025);
		if (length > 1.0E-8) {
			x = x / length * speed;
			z = z / length * speed;
		}
		double vertical = jump == shift ? 0.0 : jump ? 0.9 : -0.9;
		return new Motion(x, vertical, z);
	}
}
