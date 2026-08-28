package com.powers.audio;

import java.util.Optional;

/** Pure listener-side classification and conservative mix-headroom rules. */
public final class LayeredAudioRules {
	private static final float MAX_GAIN = 0.90F;

	private LayeredAudioRules() {
	}

	public static Optional<ResolvedLayer> resolve(LayeredAudioCue cue, double distance,
			boolean obstructed, boolean reducedTinnitus, float baseGain, int concurrent) {
		if (cue == null || !Double.isFinite(distance) || distance < 0.0
				|| !Float.isFinite(baseGain) || baseGain <= 0.0F) return Optional.empty();
		LayeredAudioLayer layer = cue.profile().layer(distance).orElse(null);
		if (layer == null) return Optional.empty();
		if (obstructed) layer = layer.softer();
		float obstructionGain = obstructed ? 0.45F : 1.0F;
		float requested = Math.clamp(baseGain, 0.0F, 4.0F) * obstructionGain;
		float gain = Math.min(requested, headroom(concurrent, MAX_GAIN));
		return Optional.of(new ResolvedLayer(layer, gain, obstructionGain,
				reducedTinnitus && cue.tinnitusSensitive()));
	}

	public static float headroom(int concurrent, float cap) {
		if (!Float.isFinite(cap) || cap <= 0.0F) return 0.0F;
		int count = Math.clamp(concurrent, 1, 8);
		return Math.min(cap, MAX_GAIN) / (float) Math.sqrt(count);
	}

	public record ResolvedLayer(LayeredAudioLayer layer, float gain,
			float obstructionGain, boolean reducedTinnitus) {
	}
}
