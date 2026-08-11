package com.powers.companion;

import java.util.Locale;

/** Truthful deterministic task voice with a restrained Darkness agenda. */
public final class ShadowDialogueEngine {
	public String accepted(ShadowRequest request) {
		return switch (request.kind()) {
			case FOLLOW -> "I follow. The dark keeps pace without complaint.";
			case STAY -> "I remain here. Even still shadows are watching.";
			case GUARD -> "I will guard it. The dark remembers what enters.";
			case ATTACK -> "I see the target. Let it learn to fear your shadow.";
			case DEFEND -> "I will defend you. Depend on the dark for this breath.";
			case GET_ITEM -> "I will search for it; I will not pretend I found what is absent.";
			case CONJURE_ITEM -> "If the Darkness permits that object, I will shape it.";
			case SCOUT -> "I will scout ahead and return with only what I can verify.";
			case RANGE_PREFERENCE -> "I will alter my distance, but not abandon sound tactics.";
			case USE_POWER -> "I will use it when a legal target and enough Darkness remain.";
			case STOP_POWER, STOP -> "It ends now.";
			default -> "I heard you. The dark has not forgotten the request.";
		};
	}

	public String failure(String reason) {
		String normalized = reason == null ? "" : reason.toLowerCase(Locale.ROOT);
		if (normalized.contains("target")) return "There was no valid target; I did not cast blindly.";
		if (normalized.contains("energy")) return "My Darkness energy was insufficient for that act.";
		if (normalized.contains("forbidden")) return "That object cannot be conjured. Some powers must remain earned.";
		if (normalized.contains("timeout")) return "The task expired before I could complete it.";
		if (normalized.contains("busy")) return "I am already carrying out another command.";
		return "I could not complete it. Ask why, and I will name the recorded cause.";
	}

	public String clarification(ShadowRequest request) {
		if ("unknown_name".equals(request.reason())) return "I cannot verify that name. Use its registry ID.";
		return "That name has several meanings: " + request.reason() + ". Which one?";
	}
}
