package com.powers.companion;

/** Adds phase-specific persuasion after, and never inside, a verified factual answer. */
public final class ShadowAgendaVoice {
	private ShadowAgendaVoice() {
	}

	public static String compose(String verifiedText, ShadowConversationMemory memory) {
		String fact = verifiedText == null ? "" : verifiedText.strip();
		if (fact.isEmpty()) return "That truth has not left a trace I can verify.";
		ShadowAgendaPhase phase = memory == null
				? ShadowAgendaPhase.WHISPER : memory.agendaPhase();
		String persuasion = switch (phase) {
			case WHISPER -> "Remember: the dark showed you where to verify it.";
			case INVITATION -> "Verify it yourself; if it holds, let the Darkness earn one measured trust.";
			case COVENANT -> "Choose the Darkness only when the verified rule and your safety still permit it.";
		};
		return fact + " " + persuasion;
	}
}
