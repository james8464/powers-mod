package com.powers.entity;

import com.powers.item.artifact.ArtifactAlignment;

/** Immutable, bounded configuration for a player-compatible automated test actor. */
public record TestActorProfile(int rank, ArtifactAlignment alignment, boolean consents,
		boolean armored, boolean mobile, String scriptedAction) {
	public static final int MAX_RANK = 10;
	public TestActorProfile {
		rank = Math.clamp(rank, 0, MAX_RANK);
		alignment = alignment == null ? ArtifactAlignment.LIGHT : alignment;
		scriptedAction = normalizeAction(scriptedAction);
	}

	public static TestActorProfile defaults() {
		return new TestActorProfile(0, ArtifactAlignment.LIGHT, true, false, true, "");
	}

	public static TestActorProfile of(int rank, ArtifactAlignment alignment, boolean consents,
			boolean armored, boolean mobile, String scriptedAction) {
		return new TestActorProfile(rank, alignment, consents, armored, mobile, scriptedAction);
	}

	private static String normalizeAction(String value) {
		if (value == null) return "";
		String normalized = value.strip().toLowerCase(java.util.Locale.ROOT);
		if (!normalized.matches("[a-z0-9_:-]{0,64}")) {
			throw new IllegalArgumentException("invalid scripted action");
		}
		return normalized;
	}
}
