package com.powers.config;

import java.util.List;

/** Bounded, value-free summary of config defaults and clamps applied during validation. */
public record ConfigValidationReport(long revision, int adjustments, int dropped, List<Entry> entries) {
	public static final int MAX_ENTRIES = 64;

	public enum Kind {
		CLAMPED,
		DEFAULTED
	}

	public ConfigValidationReport {
		revision = Math.max(0L, revision);
		adjustments = Math.max(0, adjustments);
		dropped = Math.max(0, dropped);
		entries = List.copyOf(entries);
	}

	public static ConfigValidationReport of(long revision, List<Entry> entries) {
		List<Entry> safe = entries == null ? List.of() : List.copyOf(entries);
		int retained = Math.min(MAX_ENTRIES, safe.size());
		return new ConfigValidationReport(revision, safe.size(), safe.size() - retained,
				safe.subList(0, retained));
	}

	public static ConfigValidationReport empty() {
		return of(0L, List.of());
	}

	public String summary() {
		return "revision=" + revision + "; adjustments=" + adjustments
				+ "; retained=" + entries.size() + "; dropped=" + dropped;
	}

	public record Entry(String path, Kind kind) {
		public Entry {
			path = path == null || path.isBlank() ? "unknown" : path.substring(0, Math.min(80, path.length()));
			kind = java.util.Objects.requireNonNull(kind, "kind");
		}
	}
}
