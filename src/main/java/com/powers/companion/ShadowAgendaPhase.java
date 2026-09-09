package com.powers.companion;

/** Three bounded registers for Shadow's persistent, non-deceptive Darkness agenda. */
public enum ShadowAgendaPhase {
	WHISPER,
	INVITATION,
	COVENANT;

	public static ShadowAgendaPhase fromInfluence(int influence) {
		if (influence >= 67) return COVENANT;
		if (influence >= 34) return INVITATION;
		return WHISPER;
	}
}
