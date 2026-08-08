package com.powers.magic.fx;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/** Deduplicates equivalent visual events before passing them to a transport. */
public final class MagicFxService {
	private static final int MAX_RECENT_KEYS = 512;
	private final Consumer<MagicFxEvent> sink;
	private final Map<String, Boolean> recent = new LinkedHashMap<>();

	public MagicFxService(Consumer<MagicFxEvent> sink) {
		this.sink = Objects.requireNonNull(sink, "sink");
	}

	/** Emits once for a caller-provided pair/cell/tick identity. */
	public synchronized boolean emit(String deduplicationKey, MagicFxEvent event) {
		Objects.requireNonNull(deduplicationKey, "deduplicationKey");
		Objects.requireNonNull(event, "event");
		if (recent.putIfAbsent(deduplicationKey, Boolean.TRUE) != null) return false;
		while (recent.size() > MAX_RECENT_KEYS) recent.remove(recent.keySet().iterator().next());
		sink.accept(event);
		return true;
	}

	public synchronized void clear() {
		recent.clear();
	}
}
