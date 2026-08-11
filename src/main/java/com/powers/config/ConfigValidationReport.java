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

	public List<String> operatorLines() {
		return entries.stream().map(entry -> entry.operatorLine(revision)).toList();
	}

	public record Entry(String path, Kind kind, String original, String sanitized, String reason) {
		public Entry(String path, Kind kind) {
			this(path, kind, kind == Kind.DEFAULTED ? "<missing-or-invalid>" : "<out-of-range>",
					kind == Kind.DEFAULTED ? "<default>" : "<bounded>",
					kind == Kind.DEFAULTED ? "default_substitution" : "sanitized");
		}

		public Entry {
			path = path == null || path.isBlank() ? "unknown" : bounded(path);
			kind = java.util.Objects.requireNonNull(kind, "kind");
			original = bounded(original);
			sanitized = bounded(sanitized);
			reason = bounded(reason);
		}

		public String operatorLine(long revision) {
			return "revision=" + Math.max(0L, revision) + "; field=" + path
					+ "; original=" + original + "; sanitized=" + sanitized + "; reason=" + reason;
		}

		private static String bounded(String value) {
			if (value == null || value.isBlank()) return "<none>";
			StringBuilder safe = new StringBuilder(Math.min(80, value.length()));
			for (int index = 0; index < value.length() && safe.length() < 80; index++) {
				char character = value.charAt(index);
				safe.append(character >= 0x20 && character <= 0x7e ? character : '_');
			}
			return safe.toString();
		}
	}
}
