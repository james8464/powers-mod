package com.powers.audit;

import java.util.Locale;
import java.util.Objects;

/** One log-safe structured event; identifying fields never enter aggregate snapshots. */
public record OperatorAuditEvent(OperatorAuditAction action, OperatorAuditResult result,
		String actor, String subject, String detail) {
	private static final int IDENTITY_LIMIT = 32;
	private static final int DETAIL_LIMIT = 64;

	public OperatorAuditEvent {
		action = Objects.requireNonNull(action, "action");
		result = Objects.requireNonNull(result, "result");
		actor = sanitize(actor, IDENTITY_LIMIT);
		subject = sanitize(subject, IDENTITY_LIMIT);
		detail = sanitize(detail, DETAIL_LIMIT);
	}

	public String structuredLine() {
		return "powers_operator_audit action=" + key(action) + " result=" + key(result)
				+ " actor=" + actor + " subject=" + subject + " detail=" + detail;
	}

	private static String key(Enum<?> value) {
		return value.name().toLowerCase(Locale.ROOT);
	}

	private static String sanitize(String value, int limit) {
		if (value == null || value.isBlank()) return "none";
		StringBuilder safe = new StringBuilder(Math.min(limit, value.length()));
		for (int index = 0; index < value.length() && safe.length() < limit; index++) {
			char character = value.charAt(index);
			safe.append(character >= 0x21 && character <= 0x7e && character != '='
					? character : '_');
		}
		return safe.toString();
	}
}
