package com.powers.client.acceptance;

import java.util.function.Function;

/** Explicit development-only configuration for one isolated multiplayer QA client. */
public record AcceptanceClientConfig(boolean enabled, String role, String server, String script) {
	public static AcceptanceClientConfig resolve(boolean development,
			Function<String, String> property) {
		String role = normalized(property.apply("powers.qa.role"));
		String server = normalized(property.apply("powers.qa.server"));
		String script = normalized(property.apply("powers.qa.script"));
		boolean enabled = development && !role.isEmpty() && !server.isEmpty();
		return new AcceptanceClientConfig(enabled, role, server, script);
	}

	private static String normalized(String value) {
		return value == null ? "" : value.strip();
	}
}
