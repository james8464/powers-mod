package com.powers.magic.fx;

import java.util.Locale;

/** Bounded geometry families understood by the client presentation engine. */
public enum FxMotif {
	RING,
	SPIRAL,
	TETHER,
	FORK,
	SHARD,
	GLYPH,
	ROOT,
	ECLIPSE,
	FRACTURE;

	/** Maps exhaustive interaction names onto a finite renderer vocabulary. */
	public static FxMotif fromCue(String cue) {
		String value = cue.toLowerCase(Locale.ROOT);
		if (value.contains("eclipse") || value.contains("veil")) return ECLIPSE;
		if (value.contains("fracture") || value.contains("interference")) return FRACTURE;
		if (value.contains("rift") || value.contains("anchor") || value.contains("chain")) return TETHER;
		if (value.contains("storm") || value.contains("lightning")) return FORK;
		if (value.contains("steam") || value.contains("rain") || value.contains("bloom")) return SPIRAL;
		if (value.contains("amethyst") || value.contains("shard")) return SHARD;
		if (value.contains("root") || value.contains("ground")) return ROOT;
		if (value.contains("resonance") || value.contains("weave")) return RING;
		return GLYPH;
	}
}
