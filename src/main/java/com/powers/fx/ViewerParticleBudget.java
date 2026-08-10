package com.powers.fx;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Counts particles after recipient expansion, enforcing both a server-wide and
 * per-viewer ceiling. This prevents a nominal ten-particle burst from becoming
 * an unaccounted ten-times-player-count network spike.
 */
public final class ViewerParticleBudget {
	private final int serverLimit;
	private final int viewerLimit;
	private final double rangeSquared;
	private final Map<UUID, Integer> viewerUse = new HashMap<>();
	private long tick = Long.MIN_VALUE;
	private int serverUse;

	public ViewerParticleBudget(int serverLimit, int viewerLimit, double range) {
		this.serverLimit = Math.max(1, serverLimit);
		this.viewerLimit = Math.max(1, viewerLimit);
		if (!Double.isFinite(range) || range <= 0.0) {
			throw new IllegalArgumentException("Particle range must be finite and positive");
		}
		this.rangeSquared = range * range;
	}

	/** Returns the number that may actually be sent to this recipient. */
	public int claim(long currentTick, UUID viewer, int requested, double distanceSquared) {
		Objects.requireNonNull(viewer, "viewer");
		resetIfNeeded(currentTick);
		if (requested <= 0 || !Double.isFinite(distanceSquared) || distanceSquared > rangeSquared) return 0;
		int usedByViewer = viewerUse.getOrDefault(viewer, 0);
		int granted = Math.min(requested,
				Math.min(serverLimit - serverUse, viewerLimit - usedByViewer));
		if (granted <= 0) return 0;
		serverUse += granted;
		viewerUse.put(viewer, usedByViewer + granted);
		return granted;
	}

	private void resetIfNeeded(long currentTick) {
		if (tick == currentTick) return;
		tick = currentTick;
		serverUse = 0;
		viewerUse.clear();
	}

	int serverUsed() {
		return serverUse;
	}

	int viewerUsed(UUID viewer) {
		return viewerUse.getOrDefault(viewer, 0);
	}

	int serverLimit() {
		return serverLimit;
	}

	int viewerLimit() {
		return viewerLimit;
	}
}
