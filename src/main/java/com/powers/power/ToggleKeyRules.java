package com.powers.power;

import net.minecraft.resources.Identifier;

/** Relates canonical innate toggles to artifact-owned invocations of the same ability. */
public final class ToggleKeyRules {
	private ToggleKeyRules() {
	}

	public static boolean ownsAbility(String toggleKey, Identifier abilityId) {
		return toggleKey != null && abilityId != null
				&& (toggleKey.equals(abilityId.toString())
				|| toggleKey.endsWith("/" + abilityId.getPath()));
	}

	/** Returns whether any active invocation owns the canonical ability. */
	public static boolean anyOwnsAbility(Iterable<String> toggleKeys, Identifier abilityId) {
		if (toggleKeys == null) return false;
		for (String toggleKey : toggleKeys) {
			if (ownsAbility(toggleKey, abilityId)) return true;
		}
		return false;
	}
}
