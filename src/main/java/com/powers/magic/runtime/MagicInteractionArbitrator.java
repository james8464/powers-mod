package com.powers.magic.runtime;

import com.powers.magic.InteractionOutcome;
import com.powers.magic.InteractionResolution;

import java.util.Comparator;
import java.util.List;

/** Stable safety-first ordering for simultaneous two- and three-way magic impacts. */
public final class MagicInteractionArbitrator {
	private MagicInteractionArbitrator() {
	}

	/** Orders outcomes independently of presence insertion or chunk iteration order. */
	public static List<InteractionResolution> order(List<InteractionResolution> resolutions) {
		return resolutions.stream().sorted(resolutionOrder()).toList();
	}

	/** Preserves each event while applying the same deterministic arbitration order. */
	public static List<MagicReactionEvent> orderEvents(List<MagicReactionEvent> events) {
		return events.stream().sorted(Comparator
				.comparing(MagicReactionEvent::resolution, resolutionOrder())
				.thenComparing(event -> event.existing().action().value())
				.thenComparing(event -> event.existing().id().value())).toList();
	}

	private static Comparator<InteractionResolution> resolutionOrder() {
		return Comparator.comparingInt((InteractionResolution resolution) -> priority(resolution.outcome()))
				.thenComparing(resolution -> resolution.cue().motif())
				.thenComparing(InteractionResolution::mechanics);
	}

	private static int priority(InteractionOutcome outcome) {
		return switch (outcome) {
			case CANCEL, CONSUME, SHATTER, REFLECT -> 0;
			case CONTEST, TRANSFORM -> 1;
			case DESTABILIZE, DAMPEN -> 2;
			case AMPLIFY, RESONATE -> 3;
			case COEXIST -> 4;
		};
	}
}
