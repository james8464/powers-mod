package com.powers.testing.network;

import java.util.List;

/** One hard admission and scheduler-work budget shared by every fault session on a server. */
final class PacketFaultServerBudget {
	static final int GLOBAL_QUEUE_LIMIT = 32_768;
	static final int GLOBAL_WORK_PER_TICK = 4_096;

	private int queueDepth;
	private int nextSession;

	synchronized boolean tryAcquire() {
		if (queueDepth >= GLOBAL_QUEUE_LIMIT) return false;
		queueDepth++;
		return true;
	}

	synchronized void release(int count) {
		if (count < 0 || count > queueDepth) {
			throw new IllegalStateException("Packet-fault queue accounting underflow");
		}
		queueDepth -= count;
	}

	synchronized int queueDepth() {
		return queueDepth;
	}

	void tick(List<PacketFaultEngine> engines, long currentTick) {
		if (engines.isEmpty()) return;
		int size = engines.size();
		int start = Math.floorMod(nextSession++, size);
		int remaining = GLOBAL_WORK_PER_TICK;
		for (int offset = 0; offset < size && remaining > 0; offset++) {
			int sessionsLeft = size - offset;
			int fairShare = Math.max(1, remaining / sessionsLeft);
			remaining -= engines.get((start + offset) % size).tick(currentTick, fairShare);
		}
	}
}
