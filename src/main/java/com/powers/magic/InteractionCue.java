package com.powers.magic;

import java.util.Objects;

/**
 * Compact, colour-plus-shape-plus-sound presentation instruction for a
 * resolved collision. Client choreography expands this deterministic cue
 * locally rather than receiving a particle array from the server.
 *
 * @param motif named geometry family
 * @param sound named sound family
 * @param primaryColor first RGB colour
 * @param secondaryColor second RGB colour
 * @param glyphSeed deterministic geometry seed
 * @param intensity bounded presentation strength from 1 to 5
 */
public record InteractionCue(String motif, String sound, int primaryColor, int secondaryColor,
		int glyphSeed, int intensity) {
	/** Validates network-safe cue data. */
	public InteractionCue {
		Objects.requireNonNull(motif, "motif");
		Objects.requireNonNull(sound, "sound");
		if (motif.isBlank() || sound.isBlank()) {
			throw new IllegalArgumentException("Interaction cue requires motif and sound");
		}
		if ((primaryColor & 0xFF000000) != 0 || (secondaryColor & 0xFF000000) != 0) {
			throw new IllegalArgumentException("Interaction cue colours must be 24-bit RGB");
		}
		if (intensity < 1 || intensity > 5) {
			throw new IllegalArgumentException("Interaction cue intensity must be 1..5");
		}
	}

	/** Returns whether both non-colour accessibility channels are populated. */
	public boolean isComplete() {
		return !motif.isBlank() && !sound.isBlank();
	}
}
