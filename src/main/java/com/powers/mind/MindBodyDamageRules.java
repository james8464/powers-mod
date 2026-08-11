package com.powers.mind;

/** Pure damage policy shared by the physical proxy and detached mind avatar. */
public final class MindBodyDamageRules {
	private MindBodyDamageRules() {
	}

	public static boolean avatarMayTakeDamage(boolean detached) {
		return !detached;
	}

	public static boolean proxyDamageIsFatal(float incomingDamage, float ownerHealth) {
		return Float.isFinite(incomingDamage) && Float.isFinite(ownerHealth)
				&& incomingDamage >= 0.0F && ownerHealth > 0.0F && incomingDamage >= ownerHealth;
	}
}
