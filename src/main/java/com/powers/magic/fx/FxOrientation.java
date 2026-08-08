package com.powers.magic.fx;

import java.util.Objects;

/** Selects how a local-space magic point cloud occupies the world. */
public enum FxOrientation {
	AUTO,
	NATIVE,
	GROUND,
	BILLBOARD;

	/** Resolves motif-aware orientation while preserving explicit placement. */
	public FxOrientation resolve(FxMotif motif) {
		Objects.requireNonNull(motif, "motif");
		if (this != AUTO) return this;
		return switch (motif) {
			case GLYPH, ECLIPSE, FORK -> BILLBOARD;
			default -> NATIVE;
		};
	}
}
