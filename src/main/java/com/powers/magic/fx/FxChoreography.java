package com.powers.magic.fx;

import java.util.Objects;
import java.util.Optional;

/** Pure timing policy shared by cast and interaction presentation. */
public final class FxChoreography {
	private FxChoreography() {
	}

	/** Returns the frame due at an event age, or empty between readable beats. */
	public static Optional<FxFrame> frame(MagicFxKind kind, int age, boolean reducedMotion) {
		Objects.requireNonNull(kind, "kind");
		validateAge(age);
		FxFrame frame = kind == MagicFxKind.CAST ? castFrame(age) : interactionFrame(age);
		return frame == null ? Optional.empty() : Optional.of(reducedMotion ? accessible(frame) : frame);
	}

	/** Returns whether an event has completed every scheduled beat. */
	public static boolean finished(MagicFxKind kind, int age) {
		Objects.requireNonNull(kind, "kind");
		validateAge(age);
		return age >= (kind == MagicFxKind.CAST ? 17 : 18);
	}

	private static FxFrame castFrame(int age) {
		return switch (age) {
			case 0 -> frame(FxBeat.ANTICIPATION, FxMotif.GLYPH, 0.30, 0.55, 0.35);
			case 3 -> frame(FxBeat.RELEASE, null, 0.58, 0.95, 0.75);
			case 7 -> frame(FxBeat.IMPACT, null, 1.0, 1.65, 1.0);
			case 13 -> frame(FxBeat.AFTERMATH, FxMotif.SPIRAL, 0.38, 1.10, 0.45);
			default -> null;
		};
	}

	private static FxFrame interactionFrame(int age) {
		return switch (age) {
			case 0 -> frame(FxBeat.ANTICIPATION, FxMotif.RING, 0.24, 0.75, 0.30);
			case 4 -> frame(FxBeat.RELEASE, null, 0.48, 1.10, 0.70);
			case 8 -> frame(FxBeat.IMPACT, null, 1.0, 1.85, 1.0);
			case 15 -> frame(FxBeat.AFTERMATH, FxMotif.SPIRAL, 0.36, 1.25, 0.50);
			default -> null;
		};
	}

	private static FxFrame accessible(FxFrame frame) {
		Optional<FxMotif> override = frame.motifOverride().map(motif -> motif.accessible(true));
		return new FxFrame(frame.beat(), override, frame.budgetScale(),
				Math.min(frame.geometryScale(), 0.85), Math.min(frame.velocityScale(), 0.25));
	}

	private static FxFrame frame(FxBeat beat, FxMotif override, double budgetScale,
			double geometryScale, double velocityScale) {
		return new FxFrame(beat, Optional.ofNullable(override), budgetScale, geometryScale, velocityScale);
	}

	private static void validateAge(int age) {
		if (age < 0) throw new IllegalArgumentException("FX age cannot be negative");
	}
}
