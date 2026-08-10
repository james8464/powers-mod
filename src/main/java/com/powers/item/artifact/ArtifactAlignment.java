package com.powers.item.artifact;

import com.mojang.serialization.Codec;
import java.util.Locale;

/** The opposed energy and authorization path bound to a mythic artifact. */
public enum ArtifactAlignment {
	DARKNESS("darkness"),
	LIGHT("light");

	public static final Codec<ArtifactAlignment> CODEC = Codec.STRING.xmap(
			ArtifactAlignment::fromSerialized, ArtifactAlignment::serializedName);
	private final String serializedName;

	ArtifactAlignment(String serializedName) {
		this.serializedName = serializedName;
	}

	public String serializedName() {
		return serializedName;
	}

	/** Decodes only authored alignments; silently choosing one would be unsafe. */
	public static ArtifactAlignment fromSerialized(String value) {
		if (value != null) {
			String normalized = value.toLowerCase(Locale.ROOT);
			for (ArtifactAlignment alignment : values()) {
				if (alignment.serializedName.equals(normalized)) return alignment;
			}
		}
		throw new IllegalArgumentException("Unknown artifact alignment: " + value);
	}
}
