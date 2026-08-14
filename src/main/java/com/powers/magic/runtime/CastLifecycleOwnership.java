package com.powers.magic.runtime;

/** Pure authority rule for casts whose physical effect outlives the activation call. */
public final class CastLifecycleOwnership {
	private CastLifecycleOwnership() {
	}

	public static boolean mayContinue(CastSource source, boolean innateOwned,
			boolean artifactOwned, boolean artifactAuthorized) {
		return switch (source) {
			case INNATE -> innateOwned;
			case ARTIFACT -> artifactOwned && artifactAuthorized;
			case CRYSTAL, SPELL -> true;
		};
	}
}
