package com.powers.magic.runtime;

import com.powers.magic.InteractionOutcome;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class InteractionPresentationTest {
	@Test void actionBarNamesCounterAndResonanceWithNonColourIcons() {
		assertEquals("✕", InteractionPresentation.icon(InteractionOutcome.CANCEL));
		assertEquals("◎", InteractionPresentation.icon(InteractionOutcome.RESONATE));
		assertEquals("magic.powers.interaction.counter",
				InteractionPresentation.translationKey(InteractionOutcome.REFLECT));
		assertEquals("magic.powers.interaction.resonance",
				InteractionPresentation.translationKey(InteractionOutcome.AMPLIFY));
	}
}
