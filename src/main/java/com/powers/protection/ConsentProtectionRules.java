package com.powers.protection;

/** Pure ordering for self access, safe-zone protection, and optional consent. */
public final class ConsentProtectionRules {
	private ConsentProtectionRules() {
	}

	public static boolean mayTarget(boolean self, boolean safeZone,
			boolean consentRequired, boolean consentGranted) {
		if (self) return true;
		if (safeZone) return false;
		return !consentRequired || consentGranted;
	}
}
