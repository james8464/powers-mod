package com.powers.magic.runtime;

import com.powers.magic.InteractionOutcome;

/** Accessible non-colour label vocabulary for one deduplicated collision. */
public final class InteractionPresentation {
	private InteractionPresentation() { }
	public static String icon(InteractionOutcome outcome) {
		return switch (outcome) {
			case AMPLIFY, RESONATE -> "◎";
			case COEXIST -> "◇";
			default -> "✕";
		};
	}
	public static String translationKey(InteractionOutcome outcome) {
		return switch (outcome) {
			case AMPLIFY, RESONATE -> "magic.powers.interaction.resonance";
			case COEXIST -> "magic.powers.interaction.coexist";
			default -> "magic.powers.interaction.counter";
		};
	}
}
