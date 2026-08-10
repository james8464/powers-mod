package com.powers.network;

import java.util.List;

/** Pure exact-name resolution that refuses ambiguous remote-viewing targets. */
public final class NamedTargetRules {
	/** Server-visible outcome without leaking either candidate on ambiguity. */
	public enum Status {
		FOUND,
		NOT_FOUND,
		AMBIGUOUS,
		SCAN_LIMIT
	}

	/** A resolvable player username or mob custom name. */
	public record Candidate<T>(T target, String name) {
	}

	/** The unique target when found; otherwise the target is null. */
	public record Resolution<T>(Status status, T target) {
	}

	private NamedTargetRules() {
	}

	/** Matches trimmed names exactly while ignoring ordinary case differences. */
	public static boolean matches(String requestedName, String candidateName) {
		return requestedName != null && candidateName != null
				&& !requestedName.isBlank()
				&& requestedName.trim().equalsIgnoreCase(candidateName.trim());
	}

	/** Returns a target only when exactly one candidate has the requested name. */
	public static <T> Resolution<T> resolve(String requestedName, List<Candidate<T>> candidates) {
		T found = null;
		for (Candidate<T> candidate : candidates) {
			if (!matches(requestedName, candidate.name())) continue;
			if (found != null) return new Resolution<>(Status.AMBIGUOUS, null);
			found = candidate.target();
		}
		return found == null
				? new Resolution<>(Status.NOT_FOUND, null)
				: new Resolution<>(Status.FOUND, found);
	}

	/** Refuses resolution when a hard world-inspection budget was exhausted. */
	public static <T> Resolution<T> scanLimit() {
		return new Resolution<>(Status.SCAN_LIMIT, null);
	}
}
