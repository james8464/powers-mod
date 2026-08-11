package com.powers.power.abilities;

import org.junit.jupiter.api.Test;

import static com.powers.power.abilities.LightningStrikeRules.Counterplay.AMETHYST;
import static com.powers.power.abilities.LightningStrikeRules.Counterplay.FORCEFIELD;
import static com.powers.power.abilities.LightningStrikeRules.Counterplay.KINETIC_WARD;
import static com.powers.power.abilities.LightningStrikeRules.Counterplay.SAFE_ZONE;
import static com.powers.power.abilities.LightningStrikeRules.Counterplay.SANCTUARY;
import static com.powers.power.abilities.LightningStrikeRules.Counterplay.STRIKE;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** Guards the runtime body-counter adapter's protection priority. */
class LightningStrikeBodyResolverTest {
	@Test
	void protectionsResolveInNonBypassablePriorityOrder() {
		assertEquals(SAFE_ZONE, decide(false, true, true, true, true));
		assertEquals(AMETHYST, decide(true, true, true, true, true));
		assertEquals(SANCTUARY, decide(true, false, true, true, true));
		assertEquals(KINETIC_WARD, decide(true, false, false, true, true));
		assertEquals(FORCEFIELD, decide(true, false, false, false, true));
		assertEquals(STRIKE, decide(true, false, false, false, false));
	}

	private static LightningStrikeRules.Counterplay decide(boolean harmAllowed,
			boolean amethyst, boolean sanctuary, boolean kineticWard, boolean forcefield) {
		return LightningStrikeBodyResolver.decide(
				new LightningStrikeBodyResolver.Protections(harmAllowed, amethyst,
						sanctuary, kineticWard, forcefield));
	}
}
