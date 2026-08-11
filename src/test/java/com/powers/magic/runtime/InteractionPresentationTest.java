package com.powers.magic.runtime;

import com.powers.magic.InteractionOutcome;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class InteractionPresentationTest {
	@Test void actionBarNamesCounterAndResonanceWithNonColourIcons() {
		assertEquals("✕", MagicInteractionArbitrator.presentationIcon(InteractionOutcome.CANCEL));
		assertEquals("◎", MagicInteractionArbitrator.presentationIcon(InteractionOutcome.RESONATE));
		assertEquals("magic.powers.interaction.counter",
				MagicInteractionArbitrator.presentationKey(InteractionOutcome.REFLECT));
		assertEquals("magic.powers.interaction.resonance",
				MagicInteractionArbitrator.presentationKey(InteractionOutcome.AMPLIFY));
	}
}
