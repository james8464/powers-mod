package com.powers.companion;

/** Five-tick energy pulse for a manifested Shadow. */
public final class ShadowEnergyRules {
	private static final int LINKED_REFILL_PER_PULSE = 225; // 900 per second
	private static final int DARKNESS_REFILL_PER_PULSE = 80;
	private static final int PURE_LIGHT_DRAIN_PER_PULSE = 75;
	private static final int AMETHYST_DRAIN_PER_PULSE = 150;

	public record EnergyFacts(int energy, boolean linked, boolean nearDarkness,
			boolean nearPureLight, boolean amethyst, boolean testing) { }
	public record TickResult(int energy, int delta, boolean actionsSuppressed,
			boolean pureLightHarm) { }

	private ShadowEnergyRules() {
	}

	public static TickResult tick(EnergyFacts facts) {
		int before = ShadowCompanionRules.energy(facts.energy());
		if (facts.testing()) {
			return new TickResult(ShadowCompanionRules.MAX_ENERGY,
					ShadowCompanionRules.MAX_ENERGY - before, false, false);
		}
		int delta = 0;
		if (facts.linked()) delta += LINKED_REFILL_PER_PULSE;
		if (facts.nearDarkness()) delta += DARKNESS_REFILL_PER_PULSE;
		if (facts.nearPureLight()) delta -= PURE_LIGHT_DRAIN_PER_PULSE;
		if (facts.amethyst()) delta -= AMETHYST_DRAIN_PER_PULSE;
		int energy = ShadowCompanionRules.energy(before + delta);
		return new TickResult(energy, energy - before, facts.amethyst(), facts.nearPureLight());
	}
}
