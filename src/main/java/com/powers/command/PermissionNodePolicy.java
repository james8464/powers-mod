package com.powers.command;

import java.util.Optional;

/** Pure precedence rule: a present provider is authoritative, absence uses vanilla. */
public final class PermissionNodePolicy {
	private PermissionNodePolicy() { }

	public static boolean allowed(boolean vanillaAllowed, Optional<Boolean> providerDecision) {
		return providerDecision.orElse(vanillaAllowed);
	}
}
