package com.powers.audio;

/** One listener-specific distance layer from the semantic magic sound bank. */
public enum LayeredAudioLayer {
	NEAR,
	MID,
	FAR;

	/** Advances to a softer layer without ever wrapping back toward the listener. */
	public LayeredAudioLayer softer() {
		return switch (this) {
			case NEAR -> MID;
			case MID, FAR -> FAR;
		};
	}

	public String serializedName() {
		return name().toLowerCase(java.util.Locale.ROOT);
	}
}
