package com.powers.network;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Keeps the first cue latency-free while preserving bounded, ordered same-tick tails. */
public final class SemanticFxBatchAccumulator<T> {
	public static final int DEFAULT_MAX_ENTRIES = 128;

	public enum Delivery {
		IMMEDIATE,
		DEFERRED
	}

	public enum Rollover {
		NONE,
		TICK,
		CHANNEL,
		CAPACITY
	}

	public record Drain<T>(List<T> entries, int encodedBytes, long tick, Object channel) {
		public Drain {
			entries = List.copyOf(entries);
		}
	}

	public record Offer<T>(Delivery delivery, Rollover rollover, Drain<T> before) {
	}

	private final int maximumEntries;
	private final List<T> deferred;
	private boolean leadDelivered;
	private int encodedBytes;
	private long tick = Long.MIN_VALUE;
	private Object channel;

	public SemanticFxBatchAccumulator() {
		this(DEFAULT_MAX_ENTRIES);
	}

	public SemanticFxBatchAccumulator(int maximumEntries) {
		if (maximumEntries < 1) throw new IllegalArgumentException("Batch limit must be positive");
		this.maximumEntries = maximumEntries;
		this.deferred = new ArrayList<>(maximumEntries);
	}

	/**
	 * Returns any older tail before classifying the new cue. Callers send a tick/capacity
	 * rollover before the new immediate cue and discard a channel rollover as stale.
	 */
	public Offer<T> offer(long captureTick, Object captureChannel, T value, int bytes) {
		Objects.requireNonNull(captureChannel, "captureChannel");
		Objects.requireNonNull(value, "value");
		if (bytes < 0) throw new IllegalArgumentException("Encoded bytes cannot be negative");
		Rollover rollover = Rollover.NONE;
		Drain<T> before = emptyDrain();
		if (leadDelivered && (!Objects.equals(channel, captureChannel) || tick != captureTick)) {
			rollover = Objects.equals(channel, captureChannel) ? Rollover.TICK : Rollover.CHANNEL;
			before = removeTail();
			resetEpoch();
		}
		if (!leadDelivered) {
			tick = captureTick;
			channel = captureChannel;
			leadDelivered = true;
			return new Offer<>(Delivery.IMMEDIATE, rollover, before);
		}
		if (deferred.size() >= maximumEntries) {
			before = removeTail();
			return new Offer<>(Delivery.IMMEDIATE, Rollover.CAPACITY, before);
		}
		deferred.add(value);
		encodedBytes = saturatedAdd(encodedBytes, bytes);
		return new Offer<>(Delivery.DEFERRED, rollover, before);
	}

	/** Drains the current tail and clears its tick/connection epoch. */
	public Drain<T> drain() {
		Drain<T> drain = removeTail();
		resetEpoch();
		return drain;
	}

	private Drain<T> removeTail() {
		Drain<T> drain = new Drain<>(deferred, encodedBytes, tick, channel);
		deferred.clear();
		encodedBytes = 0;
		return drain;
	}

	private Drain<T> emptyDrain() {
		return new Drain<>(List.of(), 0, tick, channel);
	}

	private void resetEpoch() {
		leadDelivered = false;
		tick = Long.MIN_VALUE;
		channel = null;
	}

	private static int saturatedAdd(int current, int amount) {
		return Integer.MAX_VALUE - current < amount ? Integer.MAX_VALUE : current + amount;
	}
}
