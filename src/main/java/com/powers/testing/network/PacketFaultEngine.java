package com.powers.testing.network;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.function.Predicate;

/** Deterministic bounded scheduler independent of Fabric and wall-clock execution. */
public final class PacketFaultEngine {
	public enum OfferResult { BYPASSED, QUEUED, DROPPED }

	private record Channel(PacketFaultConnection connection, PacketFaultDirection direction) { }
	private record Stream(Channel channel, PacketFaultFamily family, String logicalKey) { }
	private record Envelope<T>(long order, long sequence, long offeredAt, long deliverAt,
			Stream stream, Predicate<T> delivery, Runnable failure, T payload, boolean duplicate) { }

	private final PacketFaultProfile profile;
	private final PacketFaultServerBudget serverBudget;
	private final PriorityQueue<Envelope<?>> queue = new PriorityQueue<>(
			Comparator.<Envelope<?>>comparingLong(Envelope::deliverAt)
					.thenComparingLong(Envelope::order));
	private final Map<Channel, Integer> channelDepth = new HashMap<>();
	private final Map<Stream, Integer> streamDepth = new HashMap<>();
	private final Map<Stream, Long> nextSequences = new HashMap<>();
	private final Map<Stream, Long> latestOffered = new HashMap<>();
	private final Map<Stream, Long> latestDelivered = new HashMap<>();
	private final Map<Stream, SeenWindow> deliveredOnce = new HashMap<>();
	private static final class SeenWindow {
		private long high = -1L;
		private long bits;

		private boolean mark(long sequence) {
			if (sequence > high) {
				long shift = sequence - high;
				bits = shift >= Long.SIZE ? 1L : (bits << shift) | 1L;
				high = sequence;
				return true;
			}
			long offset = high - sequence;
			if (offset >= Long.SIZE) return false;
			long mask = 1L << offset;
			if ((bits & mask) != 0L) return false;
			bits |= mask;
			return true;
		}
	}
	private long nextOrder;
	private long offered;
	private long dropped;
	private long duplicated;
	private long delayed;
	private long reordered;
	private long delivered;
	private long expired;
	private long overflowed;
	private long suppressedStale;
	private long cancelled;
	private long duplicateSideEffects;
	private long maximumQueueDepth;
	private long maximumAgeTicks;

	public PacketFaultEngine(PacketFaultProfile profile) {
		this(profile, new PacketFaultServerBudget());
	}

	PacketFaultEngine(PacketFaultProfile profile, PacketFaultServerBudget serverBudget) {
		this.profile = Objects.requireNonNull(profile, "profile");
		this.serverBudget = Objects.requireNonNull(serverBudget, "serverBudget");
	}

	public PacketFaultProfile profile() {
		return profile;
	}

	public <T> OfferResult offer(PacketFaultConnection connection, PacketFaultDirection direction,
			PacketFaultFamily family, long currentTick, Predicate<T> delivery,
			Runnable failure, T payload) {
		return offer(connection, direction, family, family.name(), currentTick, delivery, failure, payload);
	}

	public <T> OfferResult offer(PacketFaultConnection connection, PacketFaultDirection direction,
			PacketFaultFamily family, String logicalKey, long currentTick, Predicate<T> delivery,
			Runnable failure, T payload) {
		Objects.requireNonNull(connection, "connection");
		Objects.requireNonNull(direction, "direction");
		Objects.requireNonNull(family, "family");
		Objects.requireNonNull(logicalKey, "logicalKey");
		if (logicalKey.length() > PacketFaultStreams.MAX_KEY_LENGTH) {
			throw new IllegalArgumentException("Packet fault stream key is too long");
		}
		Objects.requireNonNull(delivery, "delivery");
		Objects.requireNonNull(failure, "failure");
		if (!profile.targets(direction, family)) {
			delivery.test(payload);
			return OfferResult.BYPASSED;
		}

		offered++;
		Channel channel = new Channel(connection, direction);
		Stream stream = new Stream(channel, family, logicalKey);
		long sequence = nextSequences.merge(stream, 1L, Long::sum) - 1L;
		if (family.semantics() == PacketFaultFamily.Semantics.CURRENT_ONLY) {
			latestOffered.put(stream, sequence);
		}
		long decision = mix(profile.seed(), connection.owner().getMostSignificantBits()
				^ connection.owner().getLeastSignificantBits(), direction.name().hashCode(),
				family.name().hashCode(), logicalKey.hashCode(), offered);
		if (sample(decision, profile.lossPerTenThousand())) {
			dropped++;
			failure.run();
			prune(stream);
			return OfferResult.DROPPED;
		}

		if (!enqueue(channel, stream, sequence, currentTick, decision, delivery, failure, payload, false, true)) {
			prune(stream);
			return OfferResult.DROPPED;
		}
		if (sample(Long.rotateLeft(decision, 23), profile.duplicatePerTenThousand())) {
			duplicated++;
			enqueue(channel, stream, sequence, currentTick, ~decision, delivery, failure, payload, true, false);
		}
		return OfferResult.QUEUED;
	}

	private <T> boolean enqueue(Channel channel, Stream stream, long sequence, long currentTick,
			long decision, Predicate<T> delivery, Runnable failure, T payload,
			boolean duplicate, boolean failOnOverflow) {
		int depth = channelDepth.getOrDefault(channel, 0);
		if (depth >= profile.queueLimit() || !serverBudget.tryAcquire()) {
			overflowed++;
			if (failOnOverflow) failure.run();
			return false;
		}
		int jitter = profile.reorderWindowTicks() == 0 ? 0
				: profile.reorderWindowTicks() - (int) Math.floorMod(sequence,
						profile.reorderWindowTicks() + 1L);
		long requestedDelivery = currentTick + profile.delayTicks() + jitter;
		long expireAt = currentTick + profile.lifetimeTicks() + 1L;
		long deliverAt = Math.min(requestedDelivery, expireAt);
		queue.add(new Envelope<>(nextOrder++, sequence, currentTick, deliverAt,
				stream, delivery, failure, payload, duplicate));
		channelDepth.put(channel, depth + 1);
		streamDepth.merge(stream, 1, Integer::sum);
		maximumQueueDepth = Math.max(maximumQueueDepth, queue.size());
		if (requestedDelivery > currentTick) delayed++;
		return true;
	}

	/** Delivers at most the configured work allowance for each connection and direction. */
	public void tick(long currentTick) {
		tick(currentTick, PacketFaultServerBudget.GLOBAL_WORK_PER_TICK);
	}

	int tick(long currentTick, int maximumWork) {
		Map<Channel, Integer> work = new HashMap<>();
		List<Envelope<?>> deferred = new ArrayList<>();
		int globalWork = 0;
		while (globalWork < maximumWork
				&& !queue.isEmpty() && queue.peek().deliverAt() <= currentTick) {
			Envelope<?> envelope = queue.poll();
			globalWork++;
			long age = Math.max(0L, currentTick - envelope.offeredAt());
			maximumAgeTicks = Math.max(maximumAgeTicks, age);
			if (age > profile.lifetimeTicks()) {
				expired++;
				try {
					fail(envelope);
				} finally {
					decrement(envelope.stream());
				}
				continue;
			}
			Channel channel = envelope.stream().channel();
			int used = work.getOrDefault(channel, 0);
			if (used >= profile.workPerTick()) {
				deferred.add(reschedule(envelope, currentTick + 1L));
				continue;
			}
			work.put(channel, used + 1);
			try {
				deliver(envelope);
			} finally {
				decrement(envelope.stream());
			}
		}
		queue.addAll(deferred);
		return globalWork;
	}

	private void deliver(Envelope<?> envelope) {
		Stream stream = envelope.stream();
		long previous = latestDelivered.getOrDefault(stream, -1L);
		if (stream.family().semantics() == PacketFaultFamily.Semantics.CURRENT_ONLY
				&& (envelope.sequence() < latestOffered.getOrDefault(stream, envelope.sequence())
						|| envelope.sequence() <= previous)) {
			suppressedStale++;
			if (envelope.sequence() < latestOffered.getOrDefault(stream, envelope.sequence())
					|| envelope.sequence() < previous) reordered++;
			return;
		}
		SeenWindow onceWindow = stream.family().semantics() == PacketFaultFamily.Semantics.ONCE
				? deliveredOnce.computeIfAbsent(stream, ignored -> new SeenWindow()) : null;
		if (onceWindow != null && !onceWindow.mark(envelope.sequence())) {
			suppressedStale++;
			return;
		}
		if (!accept(envelope)) {
			cancelled++;
			fail(envelope);
			return;
		}
		if (stream.family().semantics() == PacketFaultFamily.Semantics.CURRENT_ONLY) {
			latestDelivered.put(stream, envelope.sequence());
		}
		if (envelope.duplicate() && stream.family().semantics() != PacketFaultFamily.Semantics.EVERY) {
			duplicateSideEffects++;
		}
		delivered++;
	}

	@SuppressWarnings("unchecked")
	private static <T> boolean accept(Envelope<?> envelope) {
		Envelope<T> typed = (Envelope<T>) envelope;
		return typed.delivery().test(typed.payload());
	}

	private static void fail(Envelope<?> envelope) {
		envelope.failure().run();
	}

	private static <T> Envelope<T> reschedule(Envelope<T> envelope, long deliverAt) {
		return new Envelope<>(envelope.order(), envelope.sequence(), envelope.offeredAt(), deliverAt,
				envelope.stream(), envelope.delivery(), envelope.failure(), envelope.payload(), envelope.duplicate());
	}

	private void decrement(Stream stream) {
		serverBudget.release(1);
		Channel channel = stream.channel();
		channelDepth.computeIfPresent(channel, (ignored, depth) -> depth <= 1 ? null : depth - 1);
		streamDepth.computeIfPresent(stream, (ignored, depth) -> depth <= 1 ? null : depth - 1);
		prune(stream);
	}

	private void prune(Stream stream) {
		if (streamDepth.containsKey(stream)) return;
		nextSequences.remove(stream);
		latestOffered.remove(stream);
		latestDelivered.remove(stream);
		deliveredOnce.remove(stream);
	}

	public int clear(PacketFaultConnection connection) {
		List<Envelope<?>> removed = queue.stream()
				.filter(envelope -> envelope.stream().channel().connection().equals(connection)).toList();
		queue.removeAll(removed);
		serverBudget.release(removed.size());
		for (Envelope<?> envelope : removed) {
			cancelled++;
			fail(envelope);
		}
		channelDepth.keySet().removeIf(channel -> channel.connection().equals(connection));
		streamDepth.keySet().removeIf(stream -> stream.channel().connection().equals(connection));
		nextSequences.keySet().removeIf(stream -> stream.channel().connection().equals(connection));
		latestOffered.keySet().removeIf(stream -> stream.channel().connection().equals(connection));
		latestDelivered.keySet().removeIf(stream -> stream.channel().connection().equals(connection));
		deliveredOnce.keySet().removeIf(stream -> stream.channel().connection().equals(connection));
		return removed.size();
	}

	public int cancelAll() {
		List<Envelope<?>> removed = List.copyOf(queue);
		queue.clear();
		serverBudget.release(removed.size());
		channelDepth.clear();
		streamDepth.clear();
		nextSequences.clear();
		latestOffered.clear();
		latestDelivered.clear();
		deliveredOnce.clear();
		for (Envelope<?> envelope : removed) {
			cancelled++;
			fail(envelope);
		}
		return removed.size();
	}

	public void reset() {
		try {
			cancelAll();
		} finally {
			nextOrder = 0L;
			offered = dropped = duplicated = delayed = reordered = delivered = expired = overflowed = 0L;
			suppressedStale = cancelled = duplicateSideEffects = maximumQueueDepth = maximumAgeTicks = 0L;
		}
	}

	public int queueDepth() {
		return queue.size();
	}

	int retainedStreamCount() {
		return streamDepth.size() + nextSequences.size() + latestOffered.size()
				+ latestDelivered.size() + deliveredOnce.size();
	}

	public PacketFaultMetrics snapshot() {
		return new PacketFaultMetrics(offered, dropped, duplicated, delayed, reordered, delivered,
				expired, overflowed, suppressedStale, cancelled, duplicateSideEffects,
				maximumQueueDepth, maximumAgeTicks);
	}

	private static boolean sample(long value, int perTenThousand) {
		return perTenThousand > 0 && Math.floorMod(value, 10_000L) < perTenThousand;
	}

	private static long mix(long seed, long owner, int direction, int family,
			int logicalKey, long offerOrdinal) {
		long value = seed ^ owner ^ ((long) direction << 61) ^ ((long) family << 48)
				^ ((long) logicalKey * 0xD6E8FEB86659FD93L)
				^ (offerOrdinal * 0x9E3779B97F4A7C15L);
		value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
		value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
		return value ^ (value >>> 31);
	}
}
