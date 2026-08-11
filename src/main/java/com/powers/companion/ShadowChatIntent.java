package com.powers.companion;

import java.util.Locale;
import java.util.Set;

/** A bounded, side-effect-free interpretation of an explicit {@code shadow, ...} address. */
public record ShadowChatIntent(boolean addressed, Action action, String message) {
	public static final int MAX_MESSAGE_LENGTH = 256;
	private static final String PREFIX = "shadow,";
	private static final Set<String> REVEAL = Set.of(
			"reveal yourself", "show yourself", "be seen", "reveal");
	private static final Set<String> HIDE = Set.of(
			"hide yourself", "hide", "be unseen", "conceal yourself");
	private static final Set<String> DISMISS = Set.of(
			"leave me", "dismiss", "vanish", "go away");
	private static final Set<String> SUMMON = Set.of(
			"come to me", "appear", "come", "manifest");

	public enum Action {
		NONE,
		EMPTY,
		TOO_LONG,
		QUESTION,
		REVEAL,
		HIDE,
		DISMISS,
		SUMMON
	}

	public static ShadowChatIntent parse(String raw) {
		String stripped = raw == null ? "" : raw.strip();
		if (stripped.length() < PREFIX.length()
				|| !stripped.regionMatches(true, 0, PREFIX, 0, PREFIX.length())) {
			return new ShadowChatIntent(false, Action.NONE, "");
		}
		String message = stripped.substring(PREFIX.length()).strip();
		if (message.isEmpty()) return new ShadowChatIntent(true, Action.EMPTY, "");
		if (message.length() > MAX_MESSAGE_LENGTH) {
			return new ShadowChatIntent(true, Action.TOO_LONG, "");
		}
		String command = message.toLowerCase(Locale.ROOT)
				.replaceAll("[.!?]+$", "").strip();
		Action action = REVEAL.contains(command) ? Action.REVEAL
				: HIDE.contains(command) ? Action.HIDE
				: DISMISS.contains(command) ? Action.DISMISS
				: SUMMON.contains(command) ? Action.SUMMON : Action.QUESTION;
		return new ShadowChatIntent(true, action, message);
	}
}
