package com.powers.power.abilities;

/** Pure propulsion calculation for survival flight; no creative-flight flags are involved. */
public final class FlightRules {
	public static final double MAX_AIR_ACCELERATION = 0.30;
	public static final double MAX_WATER_SPEED = 1.25;
	public static final double MAX_WATER_VERTICAL_SPEED = 0.35;
	public static final double MAX_TILT_DEGREES = 18.0;
	private static final double MAX_WATER_ACCELERATION = 0.16;
	private static final double AIR_BRAKE = 0.38;
	private static final double SONIC_SPEED = 3.0;

	public record Motion(double x, double y, double z) {
		public static final Motion ZERO = new Motion(0.0, 0.0, 0.0);

		public double horizontalSpeed() {
			return Math.sqrt(x * x + z * z);
		}
	}

	public record Input(boolean forward, boolean backward, boolean left, boolean right,
			boolean jump, boolean shift, boolean sprint) {
		public static final Input NONE = new Input(false, false, false, false,
				false, false, false);

		public boolean horizontalIntent() {
			return forward != backward || left != right;
		}
	}

	public record Environment(boolean water, int energy, int capacity, boolean previouslySonic) {}

	public enum SoundCue {
		STEADY,
		LOW_ENERGY,
		SONIC_BOOM
	}

	public record Step(Motion motion, double tiltDegrees, boolean sonic, boolean sonicCrossed,
			SoundCue soundCue) {}

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

	/**
	 * Advances one authoritative server tick without snapping or trusting client velocity.
	 * Client input selects intent only; acceleration, speed, water transition, and cues
	 * are all bounded here.
	 */
	public static Step step(Motion current, float yawDegrees, Input input,
			Environment environment, int level) {
		Motion safeCurrent = finite(current);
		Input safeInput = input == null ? Input.NONE : input;
		Environment safeEnvironment = environment == null
				? new Environment(false, 0, 0, false) : environment;
		Motion desired = motion(yawDegrees, safeInput.forward(), safeInput.backward(),
				safeInput.left(), safeInput.right(), safeInput.jump(), safeInput.shift(),
				safeInput.sprint(), level);

		double targetX = safeInput.horizontalIntent() ? desired.x() : 0.0;
		double targetZ = safeInput.horizontalIntent() ? desired.z() : 0.0;
		double maximumSpeed = safeEnvironment.water() ? MAX_WATER_SPEED
				: (safeInput.sprint() ? 3.2 : 1.15) * (1.0 + Math.clamp(level, 0, 10) * 0.025);
		double targetLength = Math.sqrt(targetX * targetX + targetZ * targetZ);
		if (targetLength > maximumSpeed && targetLength > 1.0E-8) {
			targetX = targetX / targetLength * maximumSpeed;
			targetZ = targetZ / targetLength * maximumSpeed;
		}
		double acceleration = safeEnvironment.water() ? MAX_WATER_ACCELERATION
				: safeInput.horizontalIntent() ? MAX_AIR_ACCELERATION : AIR_BRAKE;
		double[] horizontal = approachVector(safeCurrent.x(), safeCurrent.z(),
				targetX, targetZ, acceleration);

		double targetY = safeInput.jump() == safeInput.shift() ? 0.0
				: safeInput.jump() ? (safeEnvironment.water() ? MAX_WATER_VERTICAL_SPEED : 0.9)
				: -(safeEnvironment.water() ? MAX_WATER_VERTICAL_SPEED : 0.9);
		double verticalRate = safeEnvironment.water() ? MAX_WATER_ACCELERATION
				: MAX_AIR_ACCELERATION;
		double y = approach(safeCurrent.y(), targetY, verticalRate);
		Motion next = new Motion(horizontal[0], y, horizontal[1]);
		boolean sonic = !safeEnvironment.water() && (next.horizontalSpeed() >= SONIC_SPEED
				|| safeEnvironment.previouslySonic() && next.horizontalSpeed() >= SONIC_SPEED - 0.30);
		boolean crossed = sonic && !safeEnvironment.previouslySonic();
		SoundCue cue = crossed ? SoundCue.SONIC_BOOM
				: lowEnergy(safeEnvironment.energy(), safeEnvironment.capacity())
				? SoundCue.LOW_ENERGY : SoundCue.STEADY;
		double tilt = turnTilt(safeCurrent, targetX, targetZ);
		return new Step(next, tilt, sonic, crossed, cue);
	}

	private static Motion finite(Motion value) {
		if (value == null || !Double.isFinite(value.x()) || !Double.isFinite(value.y())
				|| !Double.isFinite(value.z())) return Motion.ZERO;
		return value;
	}

	private static double[] approachVector(double x, double z, double targetX,
			double targetZ, double maximumDelta) {
		double deltaX = targetX - x;
		double deltaZ = targetZ - z;
		double length = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
		if (length <= maximumDelta || length < 1.0E-8) return new double[] {targetX, targetZ};
		return new double[] {x + deltaX / length * maximumDelta,
				z + deltaZ / length * maximumDelta};
	}

	private static double approach(double value, double target, double maximumDelta) {
		return value < target ? Math.min(target, value + maximumDelta)
				: Math.max(target, value - maximumDelta);
	}

	private static boolean lowEnergy(int energy, int capacity) {
		return capacity > 0 && Math.max(0L, (long) energy) * 5L <= capacity;
	}

	private static double turnTilt(Motion current, double targetX, double targetZ) {
		double currentLength = current.horizontalSpeed();
		double targetLength = Math.sqrt(targetX * targetX + targetZ * targetZ);
		if (currentLength < 1.0E-8 || targetLength < 1.0E-8) return 0.0;
		double cross = current.x() / currentLength * targetZ / targetLength
				- current.z() / currentLength * targetX / targetLength;
		return Math.clamp(cross * MAX_TILT_DEGREES, -MAX_TILT_DEGREES, MAX_TILT_DEGREES);
	}
}
