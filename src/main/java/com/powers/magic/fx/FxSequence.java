package com.powers.magic.fx;

import java.util.List;

/** Immutable timing recipe for one semantic visual event. */
public record FxSequence(List<FxBeat> beats) {
	public FxSequence {
		beats = List.copyOf(beats);
		if (beats.isEmpty()) throw new IllegalArgumentException("An effect sequence needs at least one beat");
	}

	/** Four-beat choreography used by major casts and collisions. */
	public static FxSequence major() {
		return new FxSequence(List.of(FxBeat.ANTICIPATION, FxBeat.RELEASE,
				FxBeat.IMPACT, FxBeat.AFTERMATH));
	}
}
