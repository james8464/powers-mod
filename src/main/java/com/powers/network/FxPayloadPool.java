package com.powers.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.LinkedHashMap;
import java.util.Map;

/** Small LRU canonicalizer for immutable semantic packet records. */
public final class FxPayloadPool {
	private final int capacity;
	private final Map<CustomPacketPayload, CustomPacketPayload> values =
			new LinkedHashMap<>(64, 0.75F, true);

	public FxPayloadPool(int capacity) {
		if (capacity < 1) throw new IllegalArgumentException("Capacity must be positive");
		this.capacity = capacity;
	}

	@SuppressWarnings("unchecked")
	public synchronized <T extends CustomPacketPayload> T intern(T payload) {
		CustomPacketPayload existing = values.get(payload);
		if (existing != null) return (T) existing;
		values.put(payload, payload);
		while (values.size() > capacity) values.remove(values.keySet().iterator().next());
		return payload;
	}

	public synchronized int size() {
		return values.size();
	}

	public synchronized void clear() {
		values.clear();
	}
}
