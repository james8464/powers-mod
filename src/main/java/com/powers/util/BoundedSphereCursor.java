package com.powers.util;

import java.util.ArrayList;
import java.util.List;

/** Stateful, allocation-bounded traversal of integer positions inside a sphere. */
public final class BoundedSphereCursor {
	/** Serializable exact traversal state; the next call starts at x/y/z. */
	public record Snapshot(int radius, int x, int y, int z, boolean finished) {
	}

	/** Immutable offset relative to the caller's chosen centre. */
	public record Offset(int x, int y, int z) {
	}

	private final int radius;
	private int x;
	private int y;
	private int z;
	private boolean finished;

	public BoundedSphereCursor(int radius) {
		this.radius = Math.max(0, radius);
		this.x = -this.radius;
		this.y = -this.radius;
		this.z = -this.radius;
	}

	public BoundedSphereCursor(Snapshot snapshot) {
		this.radius = Math.max(0, snapshot.radius());
		this.x = Math.clamp(snapshot.x(), -radius, radius + 1);
		this.y = Math.clamp(snapshot.y(), -radius, radius);
		this.z = Math.clamp(snapshot.z(), -radius, radius);
		this.finished = snapshot.finished() || this.x > radius;
	}

	/** Examines at most {@code maximumChecks} cube positions and returns those in the sphere. */
	public List<Offset> take(int maximumChecks) {
		if (maximumChecks <= 0 || finished) {
			return List.of();
		}
		List<Offset> result = new ArrayList<>(maximumChecks);
		int checked = 0;
		while (!finished && checked++ < maximumChecks) {
			int currentX = x;
			int currentY = y;
			int currentZ = z;
			advance();
			long distanceSquared = (long) currentX * currentX
					+ (long) currentY * currentY + (long) currentZ * currentZ;
			if (distanceSquared <= (long) radius * radius) {
				result.add(new Offset(currentX, currentY, currentZ));
			}
		}
		return result;
	}

	public boolean finished() {
		return finished;
	}

	public Snapshot snapshot() {
		return new Snapshot(radius, x, y, z, finished);
	}

	private void advance() {
		if (++z <= radius) {
			return;
		}
		z = -radius;
		if (++y <= radius) {
			return;
		}
		y = -radius;
		if (++x > radius) {
			finished = true;
		}
	}
}
