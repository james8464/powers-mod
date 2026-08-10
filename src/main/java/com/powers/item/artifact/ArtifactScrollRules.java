package com.powers.item.artifact;

/** Exact client interception and bounded packet direction rules for artifact scrolling. */
public final class ArtifactScrollRules {
	private ArtifactScrollRules() {
	}

	public static boolean shouldIntercept(boolean screenOpen, boolean crouching,
			boolean holdingMythicArtifact, double scrollDelta) {
		return !screenOpen && crouching && holdingMythicArtifact
				&& Double.isFinite(scrollDelta) && scrollDelta != 0.0;
	}

	public static int direction(double scrollDelta) {
		if (!Double.isFinite(scrollDelta) || scrollDelta == 0.0) return 0;
		return scrollDelta > 0.0 ? 1 : -1;
	}

	public static boolean validDirection(int direction) {
		return direction == -1 || direction == 1;
	}
}
