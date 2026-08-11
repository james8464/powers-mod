package com.powers.magic.runtime;

import java.util.Objects;

/** Immutable invocation source and its one permitted scaling policy. */
public record CastContext(CastSource source, ScalingPolicy scalingPolicy) {
	public CastContext {
		Objects.requireNonNull(source, "source");
		Objects.requireNonNull(scalingPolicy, "scalingPolicy");
	}

	/** Derives policy on the server so clients cannot request rank or artifact multipliers. */
	public static CastContext forSource(CastSource source) {
		return new CastContext(source, switch (Objects.requireNonNull(source, "source")) {
			case INNATE -> ScalingPolicy.INNATE_RANK;
			case ARTIFACT -> ScalingPolicy.ARTIFACT;
			case CRYSTAL, SPELL -> ScalingPolicy.UNRANKED;
		});
	}
}
