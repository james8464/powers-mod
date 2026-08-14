package com.powers.fx;

import java.util.Objects;

/** Pure near/mid/far policy that thins samples but never edits authored event geometry. */
public final class FxLodPolicy {
	private FxLodPolicy() {
	}

	public static Decision decide(double distance, int requested,
			FxLodScope scope, FxShapeFamily family) {
		Objects.requireNonNull(scope, "scope");
		Objects.requireNonNull(family, "family");
		if (!Double.isFinite(distance) || distance < 0.0 || requested <= 0
				|| distance > scope.maximumRange()) return Decision.hidden();
		FxLodTier tier = distance <= scope.nearRange() ? FxLodTier.NEAR
				: distance <= scope.midRange() ? FxLodTier.MID : FxLodTier.FAR;
		int samples = switch (tier) {
			case NEAR -> requested;
			case MID -> Math.max(family.minimumSamples(), (int) Math.ceil(requested * 0.5));
			case FAR -> Math.max(family.minimumSamples(),
					(int) Math.ceil(requested * (family == FxShapeFamily.COLUMN ? 0.05 : 0.125)));
			case HIDDEN -> 0;
		};
		return new Decision(tier, Math.min(requested, samples), true, true);
	}

	/** One observer-specific density decision; visible decisions always preserve identity and audio. */
	public record Decision(FxLodTier tier, int particleCount,
			boolean preserveSilhouette, boolean playSignatureAudio) {
		public Decision {
			Objects.requireNonNull(tier, "tier");
			particleCount = Math.max(0, particleCount);
			if (tier == FxLodTier.HIDDEN) {
				particleCount = 0;
				preserveSilhouette = false;
				playSignatureAudio = false;
			}
		}

		public static Decision hidden() {
			return new Decision(FxLodTier.HIDDEN, 0, false, false);
		}

		public boolean visible() {
			return tier != FxLodTier.HIDDEN && particleCount > 0;
		}
	}
}
