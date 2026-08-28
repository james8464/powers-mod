package com.powers.audio;

import java.util.Optional;

/** Authored distance thresholds for intimate, ordinary, and world-scale cues. */
public enum LayeredAudioProfile {
	INTIMATE(8.0, 28.0, 72.0),
	STANDARD(12.0, 48.0, 128.0),
	WORLD(20.0, 96.0, 256.0);

	private final double nearRadius;
	private final double midRadius;
	private final double farRadius;

	LayeredAudioProfile(double nearRadius, double midRadius, double farRadius) {
		this.nearRadius = nearRadius;
		this.midRadius = midRadius;
		this.farRadius = farRadius;
	}

	public Optional<LayeredAudioLayer> layer(double distance) {
		if (!Double.isFinite(distance) || distance < 0.0 || distance > farRadius) {
			return Optional.empty();
		}
		if (distance <= nearRadius) return Optional.of(LayeredAudioLayer.NEAR);
		if (distance <= midRadius) return Optional.of(LayeredAudioLayer.MID);
		return Optional.of(LayeredAudioLayer.FAR);
	}

	public double maximumRadius() {
		return farRadius;
	}
}
