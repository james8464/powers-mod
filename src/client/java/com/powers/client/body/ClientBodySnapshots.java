package com.powers.client.body;

import com.powers.mind.BodySnapshot;
import com.powers.network.BodyProxyPackets;

import java.util.HashMap;
import java.util.Map;

/** Client-only exact-frame cache keyed by the tracked mannequin entity ID. */
public final class ClientBodySnapshots {
	private static final Map<Integer, BodySnapshot> SNAPSHOTS = new HashMap<>();

	private ClientBodySnapshots() {
	}

	public static void handle(BodyProxyPackets.BodySnapshotPayload payload) {
		if (payload.snapshotJson().isEmpty()) {
			SNAPSHOTS.remove(payload.entityId());
			return;
		}
		BodyProxyPackets.decode(payload.snapshotJson()).ifPresent(
				snapshot -> SNAPSHOTS.put(payload.entityId(), snapshot));
	}

	public static BodySnapshot get(int entityId) {
		return SNAPSHOTS.get(entityId);
	}

	public static void clear() {
		SNAPSHOTS.clear();
	}
}
