package com.powers.magic;

import java.util.Objects;

/**
 * Presentation identity shared by cast, residue, and pair-reaction effects.
 * Colours are stored as opaque RGB values; accessibility also relies on the
 * motif and sound rather than colour alone.
 *
 * @param primaryColor primary RGB colour
 * @param secondaryColor secondary RGB colour
 * @param glyphSeed deterministic seed for client geometry
 * @param motif shape-language identifier
 * @param sound sound-family identifier
 */
public record MagicSignature(int primaryColor, int secondaryColor, int glyphSeed,
		String motif, String sound) {
	/** Validates presentation fields at registry construction time. */
	public MagicSignature {
		if ((primaryColor & 0xFF000000) != 0 || (secondaryColor & 0xFF000000) != 0) {
			throw new IllegalArgumentException("Magic signature colours must be 24-bit RGB");
		}
		Objects.requireNonNull(motif, "motif");
		Objects.requireNonNull(sound, "sound");
		if (motif.isBlank() || sound.isBlank()) {
			throw new IllegalArgumentException("Magic signature motif and sound are required");
		}
	}

	/** Returns whether the signature carries every non-visual accessibility cue. */
	public boolean isComplete() {
		return !motif.isBlank() && !sound.isBlank();
	}
}
