package com.powers.item.artifact;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Persistent, hidden identity proving which mythic artifact an item stack represents. */
public record ArtifactIdentity(String artifactId, ArtifactAlignment alignment) {
	public static final int MAX_ID_LENGTH = 48;
	public static final Codec<ArtifactIdentity> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.STRING.fieldOf("id").forGetter(ArtifactIdentity::artifactId),
			ArtifactAlignment.CODEC.fieldOf("alignment").forGetter(ArtifactIdentity::alignment))
			.apply(instance, ArtifactIdentity::new));

	public ArtifactIdentity {
		if (artifactId == null || artifactId.isBlank() || artifactId.length() > MAX_ID_LENGTH
				|| !artifactId.matches("[a-z0-9_]+")) {
			throw new IllegalArgumentException("Invalid artifact identity: " + artifactId);
		}
		if (alignment == null) throw new IllegalArgumentException("Artifact alignment is required");
	}
}
