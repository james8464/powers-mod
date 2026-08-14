package com.powers.network;

import com.powers.fx.BeamFxStyle;
import com.powers.fx.ShapeFxKind;

import java.util.Objects;

/**
 * One-event immutable payload factory. It retains only the two most recent observer budgets,
 * which covers ordinary near/far fan-out without constructing one equal record per recipient.
 */
public final class FxPayloadBatch {
	private FxPayloadBatch() {
	}

	public static Beam beam(long eventId, BeamFxStyle style,
			double fromX, double fromY, double fromZ,
			double toX, double toY, double toZ, int color) {
		return new Beam(eventId, style, fromX, fromY, fromZ, toX, toY, toZ, color);
	}

	public static Shape shape(long eventId, ShapeFxKind kind,
			double x, double y, double z, double radius, double height,
			int color, double phase) {
		return new Shape(eventId, kind, x, y, z, radius, height, color, phase);
	}

	/** Per-beam fan-out retaining at most two immutable packets. */
	public static final class Beam {
		private final long eventId;
		private final BeamFxStyle style;
		private final double fromX;
		private final double fromY;
		private final double fromZ;
		private final double toX;
		private final double toY;
		private final double toZ;
		private final int color;
		private MagicFxPackets.BeamFxPayload newest;
		private MagicFxPackets.BeamFxPayload previous;

		private Beam(long eventId, BeamFxStyle style,
				double fromX, double fromY, double fromZ,
				double toX, double toY, double toZ, int color) {
			this.eventId = eventId;
			this.style = Objects.requireNonNull(style, "style");
			if (!finite(fromX, fromY, fromZ, toX, toY, toZ)) {
				throw new IllegalArgumentException("Beam endpoints must be finite");
			}
			this.fromX = fromX;
			this.fromY = fromY;
			this.fromZ = fromZ;
			this.toX = toX;
			this.toY = toY;
			this.toZ = toZ;
			this.color = color & 0xFFFFFF;
		}

		/** Returns a canonical packet for this event and clamped observer budget. */
		public MagicFxPackets.BeamFxPayload forCount(int count) {
			int bounded = Math.clamp(count, 1, 64);
			if (newest != null && newest.count() == bounded) return newest;
			if (previous != null && previous.count() == bounded) {
				MagicFxPackets.BeamFxPayload match = previous;
				previous = newest;
				newest = match;
				return match;
			}
			previous = newest;
			newest = new MagicFxPackets.BeamFxPayload(eventId, style,
					fromX, fromY, fromZ, toX, toY, toZ, bounded, color);
			return newest;
		}
	}

	/** Per-shape fan-out retaining at most two immutable packets. */
	public static final class Shape {
		private final long eventId;
		private final ShapeFxKind kind;
		private final double x;
		private final double y;
		private final double z;
		private final double radius;
		private final double height;
		private final int color;
		private final double phase;
		private MagicFxPackets.ShapeFxPayload newest;
		private MagicFxPackets.ShapeFxPayload previous;

		private Shape(long eventId, ShapeFxKind kind,
				double x, double y, double z, double radius, double height,
				int color, double phase) {
			this.eventId = eventId;
			this.kind = Objects.requireNonNull(kind, "kind");
			if (!finite(x, y, z, radius, height, phase)) {
				throw new IllegalArgumentException("Shape geometry must be finite");
			}
			this.x = x;
			this.y = y;
			this.z = z;
			this.radius = Math.clamp(radius, 0.0, 256.0);
			this.height = Math.clamp(height, -256.0, 256.0);
			this.color = color & 0xFFFFFF;
			this.phase = phase;
		}

		/** Returns a canonical packet for this event and clamped observer budget. */
		public MagicFxPackets.ShapeFxPayload forCount(int count) {
			int bounded = Math.clamp(count, 1, 640);
			if (newest != null && newest.count() == bounded) return newest;
			if (previous != null && previous.count() == bounded) {
				MagicFxPackets.ShapeFxPayload match = previous;
				previous = newest;
				newest = match;
				return match;
			}
			previous = newest;
			newest = new MagicFxPackets.ShapeFxPayload(eventId, kind,
					x, y, z, radius, height, bounded, color, phase);
			return newest;
		}
	}

	private static boolean finite(double first, double second, double third,
			double fourth, double fifth, double sixth) {
		return Double.isFinite(first) && Double.isFinite(second) && Double.isFinite(third)
				&& Double.isFinite(fourth) && Double.isFinite(fifth) && Double.isFinite(sixth);
	}
}
