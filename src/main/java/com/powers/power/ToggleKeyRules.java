package com.powers.power;

import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

/** Relates canonical innate toggles to artifact-owned invocations of the same ability. */
public final class ToggleKeyRules {
	private ToggleKeyRules() {
	}

	public static boolean ownsAbility(String toggleKey, Identifier abilityId) {
		return toggleKey != null && abilityId != null
				&& (toggleKey.equals(abilityId.toString())
				|| toggleKey.endsWith("/" + abilityId.getPath()));
	}

	/** True only for the current persistent artifact toggle namespace. */
	public static boolean isArtifactOwned(String toggleKey) {
		return toggleKey != null && (toggleKey.startsWith("artifact/darkness/")
				|| toggleKey.startsWith("artifact/light/"));
	}

	/** Returns whether any active invocation owns the canonical ability. */
	public static boolean anyOwnsAbility(Iterable<String> toggleKeys, Identifier abilityId) {
		if (toggleKeys == null) return false;
		for (String toggleKey : toggleKeys) {
			if (ownsAbility(toggleKey, abilityId)) return true;
		}
		return false;
	}

	/** Removes canonical and artifact-owned invocations of one logical toggle. */
	public static List<String> withoutAbility(Iterable<String> toggleKeys, Identifier abilityId) {
		if (toggleKeys == null) return List.of();
		List<String> retained = new ArrayList<>();
		for (String toggleKey : toggleKeys) {
			if (!ownsAbility(toggleKey, abilityId)) retained.add(toggleKey);
		}
		return List.copyOf(retained);
	}
}
