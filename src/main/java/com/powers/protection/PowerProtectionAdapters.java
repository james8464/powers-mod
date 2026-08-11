package com.powers.protection;

import com.powers.PowersMod;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Thread-safe registry for optional claim/protection integrations. */
public final class PowerProtectionAdapters {
	private static final CopyOnWriteArrayList<Entry> ENTRIES = new CopyOnWriteArrayList<>();

	private record Entry(String id, int priority, PowerProtectionAdapter adapter) {
	}

	private PowerProtectionAdapters() {
	}

	/** Registers one stable provider identity; duplicate IDs are rejected. */
	public static boolean register(String id, int priority, PowerProtectionAdapter adapter) {
		if (id == null || !id.matches("[a-z0-9_.-]{1,64}") || adapter == null) return false;
		if (ENTRIES.stream().anyMatch(entry -> entry.id().equals(id))) return false;
		ENTRIES.add(new Entry(id, priority, adapter));
		ENTRIES.sort(Comparator.comparingInt(Entry::priority).reversed().thenComparing(Entry::id));
		return true;
	}

	/** Requires unanimous provider approval and fails closed on adapter faults. */
	public static boolean allows(ProtectionQuery query) {
		if (query == null) return false;
		for (Entry entry : ENTRIES) {
			try {
				if (!entry.adapter().allows(query)) return false;
			} catch (RuntimeException failure) {
				PowersMod.LOGGER.error("Protection adapter {} failed closed for {}",
						entry.id(), query.action(), failure);
				return false;
			}
		}
		return true;
	}

	/** Stable provider IDs for diagnostics without leaking claim contents. */
	public static List<String> registeredIds() {
		return ENTRIES.stream().map(Entry::id).toList();
	}

	static void clearForTests() {
		ENTRIES.clear();
	}
}
