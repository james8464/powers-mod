package com.powers.companion;

/** Authoritative allow-list policy; testing mode never bypasses item eligibility. */
public final class ShadowConjurationRules {
	public static final int DARK_CRYSTAL_CHANNEL_TICKS = 1_200;

	public record Decision(boolean allowed, ShadowConjurationTier tier, int boundedCount,
			int cost, int channelTicks, boolean rite, String reason) { }

	private ShadowConjurationRules() {
	}

	public static Decision evaluate(ShadowConjurationFacts facts) {
		int count = Math.min(facts.requestedCount(), facts.maximumStack());
		if (facts.artifact()) return denied(facts, count, "artifact_forbidden");
		if (facts.adminOnly()) return denied(facts, count, "admin_item_forbidden");
		if (facts.spawnEgg()) return denied(facts, count, "spawn_egg_forbidden");
		if (!facts.trustedNamespace() && !facts.externalOptIn()) {
			return denied(facts, count, "external_item_not_opted_in");
		}
		if (facts.crystal()) {
			if (!facts.darkCrystal()) return denied(facts, 1, "crystal_forbidden");
			if (facts.energy() < ShadowCompanionRules.MAX_ENERGY) {
				return denied(facts, 1, "full_energy_required");
			}
			return new Decision(true, ShadowConjurationTier.MYTHIC, 1,
					ShadowCompanionRules.MAX_ENERGY, DARK_CRYSTAL_CHANNEL_TICKS,
					true, "allowed_dark_crystal_rite");
		}
		int cost = facts.testingBypass() ? 0 : facts.tier().energyCost();
		if (facts.energy() < cost) return denied(facts, count, "insufficient_energy");
		return new Decision(true, facts.tier(), count, cost, 0, false, "allowed");
	}

	private static Decision denied(ShadowConjurationFacts facts, int count, String reason) {
		return new Decision(false, facts.tier(), count, 0, 0, false, reason);
	}
}
