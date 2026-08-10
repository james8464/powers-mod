package com.powers.knowledge;

import java.util.ArrayList;
import java.util.List;

/** Bounded, insertion-ordered question history shared by every Knowledge Book screen. */
public final class KnowledgeHistory {
	public record Entry(String question, KnowledgeAnswer answer) {
	}

	private final int capacity;
	private final List<Entry> entries = new ArrayList<>();
	private int cursor = -1;

	public KnowledgeHistory(int capacity) {
		if (capacity < 1) throw new IllegalArgumentException("capacity must be positive");
		this.capacity = capacity;
	}

	public void record(String question, KnowledgeAnswer answer) {
		String normalized = question == null ? "" : question.strip();
		if (normalized.isEmpty() || answer == null) return;
		entries.removeIf(entry -> entry.question().equalsIgnoreCase(normalized));
		entries.add(new Entry(normalized, answer));
		while (entries.size() > capacity) entries.removeFirst();
		cursor = entries.size() - 1;
	}

	public Entry current() {
		return entries.isEmpty() ? null : entries.get(Math.clamp(cursor, 0, entries.size() - 1));
	}

	public Entry previous() {
		if (entries.isEmpty()) return null;
		cursor = Math.max(0, cursor - 1);
		return entries.get(cursor);
	}

	public Entry next() {
		if (entries.isEmpty()) return null;
		cursor = Math.min(entries.size() - 1, cursor + 1);
		return entries.get(cursor);
	}

	public List<Entry> entries() {
		return List.copyOf(entries);
	}

	public void clear() {
		entries.clear();
		cursor = -1;
	}
}
