package com.powers.magic.runtime;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class MagicLifecycleRulesTest {
	@Test
	void everyFormSourceAndEventHasACompleteDeterministicDecision() {
		int combinations = 0;
		for (MagicLifecycleRules.Form form : MagicLifecycleRules.Form.values()) {
			for (MagicLifecycleRules.Source source : MagicLifecycleRules.Source.values()) {
				for (MagicLifecycleRules.Event event : MagicLifecycleRules.Event.values()) {
					MagicLifecycleRules.Decision decision = MagicLifecycleRules.resolve(form, source, event);
					assertFalse(decision.mechanics().isBlank(), form + "/" + source + "/" + event);
					assertFalse(decision.motif().isBlank(), form + "/" + source + "/" + event);
					combinations++;
				}
			}
		}
		assertEquals(672, combinations);
	}

	@Test
	void detachedFatalitiesAndControlledVesselDeathHaveDifferentConsequences() {
		assertEquals(MagicLifecycleRules.Outcome.RETURN_AND_DIE,
				resolve(MagicLifecycleRules.Form.ASTRAL_AVATAR,
						MagicLifecycleRules.Source.INNATE, MagicLifecycleRules.Event.AVATAR_FATAL));
		assertEquals(MagicLifecycleRules.Outcome.RETURN_AND_DIE,
				resolve(MagicLifecycleRules.Form.REALM_AVATAR,
						MagicLifecycleRules.Source.CRYSTAL, MagicLifecycleRules.Event.BODY_FATAL));
		assertEquals(MagicLifecycleRules.Outcome.RETURN_WITH_WRATH,
				resolve(MagicLifecycleRules.Form.POSSESSION_CONTROLLER,
						MagicLifecycleRules.Source.INNATE, MagicLifecycleRules.Event.VESSEL_FATAL));
		assertEquals(MagicLifecycleRules.Outcome.RETURN_WITH_WRATH,
				resolve(MagicLifecycleRules.Form.DREAMWALK_CONTROLLER,
						MagicLifecycleRules.Source.CRYSTAL, MagicLifecycleRules.Event.VESSEL_FATAL));
	}

	@Test
	void artifactLossAndShadowDeathCannotLeaveTheirRuntimeStateActive() {
		assertEquals(MagicLifecycleRules.Outcome.DEACTIVATE_SOURCE,
				resolve(MagicLifecycleRules.Form.PHYSICAL,
						MagicLifecycleRules.Source.SHADOW_SWORD, MagicLifecycleRules.Event.SOURCE_LOST));
		assertEquals(MagicLifecycleRules.Outcome.DISMISS_SHADOW,
				resolve(MagicLifecycleRules.Form.SHADOW_REVEALED,
						MagicLifecycleRules.Source.SHADOW_SWORD, MagicLifecycleRules.Event.AVATAR_FATAL));
		assertEquals(MagicLifecycleRules.Outcome.DEACTIVATE_ALL,
				resolve(MagicLifecycleRules.Form.PHYSICAL,
						MagicLifecycleRules.Source.INNATE, MagicLifecycleRules.Event.OWNER_DEATH));
	}

	private static MagicLifecycleRules.Outcome resolve(MagicLifecycleRules.Form form,
			MagicLifecycleRules.Source source, MagicLifecycleRules.Event event) {
		return MagicLifecycleRules.resolve(form, source, event).outcome();
	}
}
