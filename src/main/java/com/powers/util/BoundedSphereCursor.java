package com.powers.util;

import java.util.ArrayList;
import java.util.List;

/** Stateful, allocation-bounded traversal of integer positions inside a sphere. */
public final class BoundedSphereCursor {
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
