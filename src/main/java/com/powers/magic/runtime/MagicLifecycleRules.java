package com.powers.magic.runtime;

import java.util.Objects;

/**
 * Exhaustive pure policy for magical forms, cast ownership, and termination
 * events. Runtime managers translate the decision into their focused cleanup.
 */
public final class MagicLifecycleRules {
	public enum Form {
		PHYSICAL, REALM_AVATAR, ASTRAL_AVATAR, TELEPORT_MARKER,
		POSSESSION_CONTROLLER, DREAMWALK_CONTROLLER, SHADOW_HIDDEN, SHADOW_REVEALED
	}

	public enum Source {
		NONE, INNATE, CRYSTAL, SPELL, SHADOW_SWORD, PARTISAN
	}

	public enum Event {
		OWNER_DEATH, AVATAR_FATAL, BODY_FATAL, VESSEL_FATAL,
		SOURCE_LOST, POWER_LOST, ENERGY_EXHAUSTED, SUPPRESSED,
		EXPIRED, TARGET_UNAVAILABLE, DIMENSION_INVALID, LOGOUT,
		SERVER_STOP, MANUAL_END
	}

	public enum Outcome {
		NONE, DIE, RETURN_AND_DIE, RETURN_WITH_WRATH, RETURN_ONLY,
		DEACTIVATE_SOURCE, DEACTIVATE_ALL, DISMISS_SHADOW, CLEANUP
	}

	public record Decision(Outcome outcome, String motif, String mechanics) {
		public Decision {
			Objects.requireNonNull(outcome, "outcome");
			Objects.requireNonNull(motif, "motif");
			Objects.requireNonNull(mechanics, "mechanics");
			if (motif.isBlank() || mechanics.isBlank()) {
				throw new IllegalArgumentException("Lifecycle decisions require presentation and mechanics");
			}
		}
	}

	private MagicLifecycleRules() {
	}

	/** Returns a complete decision for every form/source/event combination. */
	public static Decision resolve(Form form, Source source, Event event) {
		Objects.requireNonNull(form, "form");
		Objects.requireNonNull(source, "source");
		Objects.requireNonNull(event, "event");
		if (form == Form.SHADOW_REVEALED && event == Event.AVATAR_FATAL) {
			return decision(Outcome.DISMISS_SHADOW);
		}
		return switch (event) {
			case OWNER_DEATH -> decision(Outcome.DEACTIVATE_ALL);
			case LOGOUT, SERVER_STOP -> decision(Outcome.CLEANUP);
			case BODY_FATAL -> decision(detached(form) ? Outcome.RETURN_AND_DIE : Outcome.DIE);
			case AVATAR_FATAL -> decision(detached(form) ? Outcome.RETURN_AND_DIE
					: shadow(form) ? Outcome.DISMISS_SHADOW : Outcome.DIE);
			case VESSEL_FATAL -> decision(form == Form.POSSESSION_CONTROLLER
					|| form == Form.DREAMWALK_CONTROLLER
					? Outcome.RETURN_WITH_WRATH : Outcome.NONE);
			case SOURCE_LOST -> decision(shadow(form) ? Outcome.DISMISS_SHADOW
					: artifact(source) ? Outcome.DEACTIVATE_SOURCE : Outcome.NONE);
			case POWER_LOST -> decision(source == Source.INNATE
					? Outcome.DEACTIVATE_SOURCE : Outcome.NONE);
			case ENERGY_EXHAUSTED -> decision(source == Source.NONE
					? Outcome.NONE : Outcome.DEACTIVATE_SOURCE);
			case SUPPRESSED -> decision(detached(form) ? Outcome.RETURN_ONLY
					: source == Source.NONE ? Outcome.NONE : Outcome.DEACTIVATE_SOURCE);
			case EXPIRED, TARGET_UNAVAILABLE, DIMENSION_INVALID, MANUAL_END ->
					decision(detached(form) ? Outcome.RETURN_ONLY
							: shadow(form) ? Outcome.DISMISS_SHADOW : Outcome.NONE);
		};
	}

	private static boolean detached(Form form) {
		return form == Form.REALM_AVATAR || form == Form.ASTRAL_AVATAR
				|| form == Form.TELEPORT_MARKER || form == Form.POSSESSION_CONTROLLER
				|| form == Form.DREAMWALK_CONTROLLER;
	}

	private static boolean shadow(Form form) {
		return form == Form.SHADOW_HIDDEN || form == Form.SHADOW_REVEALED;
	}

	private static boolean artifact(Source source) {
		return source == Source.SHADOW_SWORD || source == Source.PARTISAN;
	}

	private static Decision decision(Outcome outcome) {
		return switch (outcome) {
			case NONE -> new Decision(outcome, "quiet_continuance",
					"The event does not own this form or source, so unrelated state continues.");
			case DIE -> new Decision(outcome, "mortal_severance",
					"Ordinary physical death proceeds through vanilla respawn.");
			case RETURN_AND_DIE -> new Decision(outcome, "soul_recoil",
					"The detached mind returns to its physical body before fatal damage and respawn.");
			case RETURN_WITH_WRATH -> new Decision(outcome, "divine_wrath",
					"A dead controlled vessel releases the living controller and invokes bounded divine wrath.");
			case RETURN_ONLY -> new Decision(outcome, "soul_recall",
					"The remote session closes and safely restores the recorded physical body.");
			case DEACTIVATE_SOURCE -> new Decision(outcome, "source_severance",
					"Only toggles and sessions owned by the lost or exhausted source are switched off.");
			case DEACTIVATE_ALL -> new Decision(outcome, "death_unbinding",
					"Death deactivates every innate and artifact-routed toggle before respawn continues.");
			case DISMISS_SHADOW -> new Decision(outcome, "shadow_collapse",
					"The current Shadow body disappears while player-keyed memories remain available.");
			case CLEANUP -> new Decision(outcome, "session_unweave",
					"Connection or server shutdown releases runtime-only state exactly once.");
		};
	}
}
