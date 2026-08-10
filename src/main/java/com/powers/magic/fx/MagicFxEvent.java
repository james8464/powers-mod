package com.powers.magic.fx;

import java.util.Objects;

/**
 * Compact semantic event sent to clients. It deliberately contains no particle
 * arrays; deterministic geometry is recreated from the motif and glyph seed.
 */
public record MagicFxEvent(MagicFxKind kind, long eventId, String motif, String sound,
		double x, double y, double z,
		int primaryColor, int secondaryColor, int glyphSeed, int intensity, int genericBeatCount) {
	public static final int MAX_INTENSITY = 5;

	public MagicFxEvent {
		Objects.requireNonNull(kind, "kind");
		Objects.requireNonNull(motif, "motif");
		Objects.requireNonNull(sound, "sound");
		if (motif.isBlank() || sound.isBlank()) throw new IllegalArgumentException("FX names cannot be blank");
		if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
			throw new IllegalArgumentException("FX position must be finite");
		}
		if ((primaryColor & 0xFF000000) != 0 || (secondaryColor & 0xFF000000) != 0) {
			throw new IllegalArgumentException("FX colours must be 24-bit RGB");
		}
		intensity = Math.clamp(intensity, 1, MAX_INTENSITY);
		if (genericBeatCount != 1 && genericBeatCount != 2
				&& genericBeatCount != 4 && genericBeatCount != 6) {
			throw new IllegalArgumentException("Semantic FX require 1, 2, 4, or 6 beats");
		}
	}

	public static MagicFxEvent interaction(long eventId, String motif, String sound,
			double x, double y, double z, int primaryColor, int secondaryColor,
			int glyphSeed, int intensity) {
		return new MagicFxEvent(MagicFxKind.INTERACTION, eventId, motif, sound, x, y, z,
				primaryColor, secondaryColor, glyphSeed, intensity, 4);
	}

	/** Creates a completed player-cast presentation event. */
	public static MagicFxEvent cast(long eventId, String motif, String sound,
			double x, double y, double z, int primaryColor, int secondaryColor,
			int glyphSeed, int intensity) {
		return cast(eventId, motif, sound, x, y, z, primaryColor, secondaryColor,
				glyphSeed, intensity, 4);
	}

	/** Creates a cast event with its authored significance-driven beat count. */
	public static MagicFxEvent cast(long eventId, String motif, String sound,
			double x, double y, double z, int primaryColor, int secondaryColor,
			int glyphSeed, int intensity, int genericBeatCount) {
		return new MagicFxEvent(MagicFxKind.CAST, eventId, motif, sound, x, y, z,
				primaryColor, secondaryColor, glyphSeed, intensity, genericBeatCount);
	}

	/** Conservative upper estimate used to keep the protocol semantic and compact. */
	public int estimatedWireBytes() {
		return 48 + motif.length() * 3 + sound.length() * 3;
	}
}
