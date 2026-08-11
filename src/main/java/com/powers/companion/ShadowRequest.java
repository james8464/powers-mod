package com.powers.companion;

/** Side-effect-free result of interpreting one explicitly addressed Shadow message. */
public record ShadowRequest(boolean addressed, Kind kind, String subject, int count,
		Range range, String original, String reason) {
	public static final int MAX_MESSAGE_LENGTH = 256;

	public enum Kind {
		NONE, EMPTY, TOO_LONG, CLARIFY, SUMMON, DISMISS, REVEAL, HIDE, FOLLOW, STAY,
		GUARD, STOP, ATTACK, DEFEND, USE_POWER, STOP_POWER, GET_ITEM, CONJURE_ITEM,
		SCOUT, DIAGNOSE, RANGE_PREFERENCE, CONVERSE
	}

	public enum Range { AUTO, CLOSE, MID, FAR }

	public ShadowRequest {
		subject = sanitize(subject, 128);
		count = Math.clamp(count, 1, 64);
		range = range == null ? Range.AUTO : range;
		original = sanitize(original, MAX_MESSAGE_LENGTH);
		reason = sanitize(reason, 512);
	}

	public static ShadowRequest unaddressed() {
		return new ShadowRequest(false, Kind.NONE, "", 1, Range.AUTO, "", "");
	}

	public static ShadowRequest simple(Kind kind, String subject) {
		return new ShadowRequest(true, kind, subject, 1, Range.AUTO, "", "");
	}

	private static String sanitize(String value, int maximum) {
		if (value == null) return "";
		String stripped = value.strip();
		return stripped.substring(0, Math.min(maximum, stripped.length()));
	}
}
