package com.powers.protection;

/** Pure ordering for ordinary consent and the Empyrean Jewel override. */
public final class ConsentOverrideRules {
	public static final int OVERRIDE_ENERGY_SURCHARGE = 40;

	public enum Decision {
		ALLOW_FREE,
		ALLOW_OVERRIDE,
		DENY_SAFE_ZONE,
		DENY_CONSENT,
		DENY_ENERGY
	}

	private ConsentOverrideRules() {
	}

	public static Decision decide(boolean self, boolean safeZone, boolean ordinaryConsent,
			boolean hasJewel, boolean enoughEnergy) {
		if (self) return Decision.ALLOW_FREE;
		if (safeZone) return Decision.DENY_SAFE_ZONE;
		if (ordinaryConsent) return Decision.ALLOW_FREE;
		if (!hasJewel) return Decision.DENY_CONSENT;
		return enoughEnergy ? Decision.ALLOW_OVERRIDE : Decision.DENY_ENERGY;
	}
}
