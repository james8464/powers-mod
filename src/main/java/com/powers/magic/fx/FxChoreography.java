package com.powers.magic.fx;

import java.util.Objects;
import java.util.Optional;

/** Pure timing policy shared by cast and interaction presentation. */
public final class FxChoreography {
	private FxChoreography() {
	}

	/** Returns the frame due at an event age, or empty between readable beats. */
	public static Optional<FxFrame> frame(MagicFxKind kind, int age, boolean reducedMotion) {
		return frame(kind, age, 4, reducedMotion);
	}

	/** Returns the authored significance-specific cast frame due at this age. */
	public static Optional<FxFrame> frame(MagicFxKind kind, int age, int beatCount,
			boolean reducedMotion) {
		Objects.requireNonNull(kind, "kind");
		validateAge(age);
		validateBeatCount(beatCount);
		FxFrame frame = kind == MagicFxKind.CAST ? castFrame(age, beatCount) : interactionFrame(age);
		return frame == null ? Optional.empty() : Optional.of(reducedMotion ? accessible(frame) : frame);
	}

	/** Returns whether an event has completed every scheduled beat. */
	public static boolean finished(MagicFxKind kind, int age) {
		return finished(kind, age, 4);
	}

	/** Returns whether the authored significance-specific sequence is complete. */
	public static boolean finished(MagicFxKind kind, int age, int beatCount) {
		Objects.requireNonNull(kind, "kind");
		validateAge(age);
		validateBeatCount(beatCount);
		return age >= (kind == MagicFxKind.CAST ? castFinishAge(beatCount) : 18);
	}

	private static FxFrame castFrame(int age, int beatCount) {
		if (beatCount == 1) {
			return age == 0 ? frame(FxBeat.RELEASE, FxMotif.RING, 0.42, 0.9, 0.6,
					0.0, FxOrientation.AUTO) : null;
		}
		if (beatCount == 2) {
			return switch (age) {
				case 0 -> frame(FxBeat.ANTICIPATION, FxMotif.GLYPH, 0.28, 0.55, 0.3,
						-0.92, FxOrientation.GROUND);
				case 7 -> frame(FxBeat.IMPACT, null, 0.8, 1.35, 0.85, 0.0, FxOrientation.AUTO);
				default -> null;
			};
		}
		if (beatCount == 6) {
			return switch (age) {
				case 0 -> frame(FxBeat.ANTICIPATION, FxMotif.GLYPH, 0.28, 0.7, 0.3,
						-0.92, FxOrientation.GROUND);
				case 3 -> frame(FxBeat.ANTICIPATION, FxMotif.RING, 0.4, 1.05, 0.45,
						-0.5, FxOrientation.GROUND);
				case 7 -> frame(FxBeat.RELEASE, null, 0.64, 1.25, 0.75, 0.0, FxOrientation.AUTO);
				case 11 -> frame(FxBeat.IMPACT, null, 1.0, 2.0, 1.0, 0.0, FxOrientation.AUTO);
				case 17 -> frame(FxBeat.AFTERMATH, FxMotif.SPIRAL, 0.5, 1.45, 0.5,
						0.35, FxOrientation.AUTO);
				case 23 -> frame(FxBeat.AFTERMATH, FxMotif.GLYPH, 0.34, 1.8, 0.3,
						0.7, FxOrientation.GROUND);
				default -> null;
			};
		}
		return switch (age) {
			case 0 -> frame(FxBeat.ANTICIPATION, FxMotif.GLYPH, 0.30, 0.55, 0.35,
					-0.92, FxOrientation.GROUND);
			case 3 -> frame(FxBeat.RELEASE, null, 0.58, 0.95, 0.75, 0.0, FxOrientation.AUTO);
			case 7 -> frame(FxBeat.IMPACT, null, 1.0, 1.65, 1.0, 0.0, FxOrientation.AUTO);
			case 13 -> frame(FxBeat.AFTERMATH, FxMotif.SPIRAL, 0.38, 1.10, 0.45,
					0.30, FxOrientation.AUTO);
			default -> null;
		};
	}

	private static int castFinishAge(int beatCount) {
		return switch (beatCount) {
			case 1 -> 5;
			case 2 -> 11;
			case 4 -> 17;
			case 6 -> 27;
			default -> throw new IllegalArgumentException("Unsupported beat count: " + beatCount);
		};
	}

	private static void validateBeatCount(int beatCount) {
		if (beatCount != 1 && beatCount != 2 && beatCount != 4 && beatCount != 6) {
			throw new IllegalArgumentException("Unsupported beat count: " + beatCount);
		}
	}

	private static FxFrame interactionFrame(int age) {
		return switch (age) {
			case 0 -> frame(FxBeat.ANTICIPATION, FxMotif.RING, 0.24, 0.75, 0.30,
					0.0, FxOrientation.AUTO);
			case 4 -> frame(FxBeat.RELEASE, null, 0.48, 1.10, 0.70, 0.0, FxOrientation.AUTO);
			case 8 -> frame(FxBeat.IMPACT, null, 1.0, 1.85, 1.0, 0.0, FxOrientation.AUTO);
			case 15 -> frame(FxBeat.AFTERMATH, FxMotif.SPIRAL, 0.36, 1.25, 0.50,
					0.0, FxOrientation.AUTO);
			default -> null;
		};
	}

	private static FxFrame accessible(FxFrame frame) {
		Optional<FxMotif> override = frame.motifOverride().map(motif -> motif.accessible(true));
		return new FxFrame(frame.beat(), override, frame.budgetScale(),
				Math.min(frame.geometryScale(), 0.85), Math.min(frame.velocityScale(), 0.25),
				frame.verticalOffset(), frame.orientation());
	}

	private static FxFrame frame(FxBeat beat, FxMotif override, double budgetScale,
			double geometryScale, double velocityScale, double verticalOffset, FxOrientation orientation) {
		return new FxFrame(beat, Optional.ofNullable(override), budgetScale, geometryScale,
				velocityScale, verticalOffset, orientation);
	}

	private static void validateAge(int age) {
		if (age < 0) throw new IllegalArgumentException("FX age cannot be negative");
	}
}
