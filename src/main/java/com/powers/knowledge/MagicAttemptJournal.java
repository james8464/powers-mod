package com.powers.knowledge;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Bounded server-thread memory used only to explain the owner's recent magic. */
public final class MagicAttemptJournal {
	public static final int CAPACITY = 16;
	public static final long RETENTION_TICKS = 20L * 60L * 5L;
	private static final MagicAttemptJournal GLOBAL = new MagicAttemptJournal();
	private final Map<UUID, Deque<MagicAttempt>> histories = new HashMap<>();
	private final Map<UUID, HintState> hints = new HashMap<>();
	private record HintState(String actionId, MagicFailureReason reason, int repeats, long lastHintTick) { }

	public static MagicAttemptJournal global() {
		return GLOBAL;
	}

	/** Records an attempt and returns true once when three identical failures repeat. */
	public boolean record(UUID owner, MagicAttempt attempt) {
		if (owner == null || attempt == null) return false;
		Deque<MagicAttempt> history = histories.computeIfAbsent(owner, ignored -> new ArrayDeque<>());
		MagicAttempt previous = history.peekLast();
		if (previous != null && previous.gameTick() == attempt.gameTick()
				&& previous.actionId().equals(attempt.actionId()) && previous.reason() == attempt.reason()) {
			return false;
		}
		history.addLast(attempt);
		while (history.size() > CAPACITY) history.removeFirst();
		if (attempt.succeeded()) {
			hints.remove(owner);
			return false;
		}
		HintState prior = hints.get(owner);
		int repeats = prior != null && prior.actionId().equals(attempt.actionId())
				&& prior.reason() == attempt.reason() ? prior.repeats() + 1 : 1;
		long lastHint = prior == null ? Long.MIN_VALUE / 2L : prior.lastHintTick();
		boolean notify = repeats >= 3 && attempt.gameTick() - lastHint >= 1_200L;
		hints.put(owner, new HintState(attempt.actionId(), attempt.reason(), repeats,
				notify ? attempt.gameTick() : lastHint));
		return notify;
	}

	/** Selects a named recent action, otherwise the newest failure, for diagnostic questions only. */
	public Optional<MagicAttempt> latestFailure(UUID owner, String question, long gameTick) {
		Deque<MagicAttempt> history = histories.get(owner);
		if (history == null || !asksWhy(question)) return Optional.empty();
		long oldest = Math.max(0L, gameTick - RETENTION_TICKS);
		while (!history.isEmpty() && history.peekFirst().gameTick() < oldest) history.removeFirst();
		if (history.isEmpty()) {
			histories.remove(owner);
			return Optional.empty();
		}
		String normalized = normalize(question);
		MagicAttempt latest = null;
		var iterator = history.descendingIterator();
		while (iterator.hasNext()) {
			MagicAttempt attempt = iterator.next();
			if (attempt.succeeded()) continue;
			if (latest == null) latest = attempt;
			String action = normalize(attempt.actionId().replace(':', ' ').replace('/', ' '));
			String leaf = attempt.actionId().replace(':', '/');
			leaf = normalize(leaf.substring(leaf.lastIndexOf('/') + 1));
			if ((!action.equals("magic") && normalized.contains(action))
					|| (!leaf.equals("magic") && normalized.contains(leaf))) return Optional.of(attempt);
		}
		return Optional.ofNullable(latest);
	}

	public int size(UUID owner) {
		Deque<MagicAttempt> history = histories.get(owner);
		return history == null ? 0 : history.size();
	}

	public boolean hasFailureAt(UUID owner, String actionId, long gameTick) {
		Deque<MagicAttempt> history = histories.get(owner);
		if (history == null) return false;
		MagicAttempt latest = history.peekLast();
		return latest != null && !latest.succeeded() && latest.gameTick() == gameTick
				&& latest.actionId().equals(MagicAttempt.canonicalAction(actionId));
	}

	public void forget(UUID owner) {
		if (owner != null) {
			histories.remove(owner);
			hints.remove(owner);
		}
	}

	public void clear() {
		histories.clear();
		hints.clear();
	}

	private static boolean asksWhy(String value) {
		String normalized = normalize(value);
		return normalized.contains("why") || normalized.contains("didnt work")
				|| normalized.contains("didn t work")
				|| normalized.contains("did not work") || normalized.contains("failed")
				|| normalized.contains("failure") || normalized.contains("what happened");
	}

	private static String normalize(String value) {
		return value == null ? "" : value.toLowerCase(Locale.ROOT)
				.replaceAll("[^a-z0-9]+", " ").strip().replaceAll(" +", " ");
	}
}
