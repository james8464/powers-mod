package com.powers.companion;

import java.util.Locale;

/** Owner accessibility/frequency preference; critical warnings are never disabled. */
public enum ShadowCalloutMode {
	CRITICAL_ONLY, NORMAL, VERBOSE;

	public static ShadowCalloutMode parse(String value) {
		if (value == null) return NORMAL;
		return switch (value.toLowerCase(Locale.ROOT)) {
			case "critical", "critical_only" -> CRITICAL_ONLY;
			case "verbose" -> VERBOSE;
			default -> NORMAL;
		};
	}

	public String savedName() {
		return name().toLowerCase(Locale.ROOT);
	}
}
