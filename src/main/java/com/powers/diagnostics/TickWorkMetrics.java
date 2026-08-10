package com.powers.diagnostics;

/** Mutable server-thread counter that retains only the current tick's work. */
public final class TickWorkMetrics {
	public record Snapshot(long tick, int particles, int packets, int entityInspections) {
	}

	private long tick = Long.MIN_VALUE;
	private int particles;
	private int packets;
	private int entityInspections;

	public void recordParticles(long currentTick, int amount) {
		advance(currentTick);
		particles = saturatedAdd(particles, amount);
	}

	public void recordPackets(long currentTick, int amount) {
		advance(currentTick);
		packets = saturatedAdd(packets, amount);
	}

	public void recordEntityInspections(long currentTick, int amount) {
		advance(currentTick);
		entityInspections = saturatedAdd(entityInspections, amount);
	}

	public Snapshot snapshot(long currentTick) {
		advance(currentTick);
		return new Snapshot(tick, particles, packets, entityInspections);
	}

	private void advance(long currentTick) {
		if (tick == currentTick) return;
		tick = currentTick;
		particles = 0;
		packets = 0;
		entityInspections = 0;
	}

	private static int saturatedAdd(int current, int amount) {
		if (amount <= 0) return current;
		long result = (long) current + amount;
		return result > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) result;
	}
}
