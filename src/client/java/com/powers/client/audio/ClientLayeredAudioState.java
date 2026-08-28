package com.powers.client.audio;

import com.powers.audio.LayeredAudioCue;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.LinkedHashMap;

/** Fixed-memory client ledger and four-tick semantic sound admission policy. */
public final class ClientLayeredAudioState {
	private static final int MAX_REMEMBERED_EVENTS = 256;
	private static final int BURST_WINDOW_TICKS = 4;
	private static final int MAX_GROUP_OFFERS = 4;
	private static final int MAX_GLOBAL_OFFERS = 8;

	private final LinkedHashMap<Long, Boolean> remembered =
			new LinkedHashMap<>(MAX_REMEMBERED_EVENTS + 1, 0.75F, true);
	private final ArrayDeque<BurstOffer> burst = new ArrayDeque<>(MAX_GLOBAL_OFFERS);
	private long newestEventId = -1L;
	private long acceptedEvents;
	private long duplicateEvents;
	private long staleEvents;
	private long admittedOffers;
	private long coalescedOffers;
	private long droppedOffers;

	/** Accepts each monotonically newer non-negative server event exactly once. */
	public boolean acceptEvent(long eventId) {
		if (remembered.containsKey(eventId)) {
			duplicateEvents++;
			return false;
		}
		if (eventId < 0 || eventId <= newestEventId) {
			staleEvents++;
			return false;
		}
		remembered.put(eventId, Boolean.TRUE);
		newestEventId = eventId;
		acceptedEvents++;
		if (remembered.size() > MAX_REMEMBERED_EVENTS) {
			Iterator<Long> eldest = remembered.keySet().iterator();
			eldest.next();
			eldest.remove();
		}
		return true;
	}

	/** Applies exact-cell coalescing followed by per-group and global admission limits. */
	public Admission admit(LayeredAudioCue cue, double x, double y, double z, long gameTime) {
		if (cue == null || gameTime < 0 || !Double.isFinite(x)
				|| !Double.isFinite(y) || !Double.isFinite(z)) {
			droppedOffers++;
			return new Admission(AdmissionResult.GLOBAL_LIMIT, 0);
		}
		prune(gameTime);
		OriginCell cell = new OriginCell(floor(x), floor(y), floor(z));
		int groupCount = 0;
		for (BurstOffer offer : burst) {
			if (offer.cue() == cue && offer.cell().equals(cell)) {
				coalescedOffers++;
				return new Admission(AdmissionResult.COALESCED, 0);
			}
			if (offer.cue().group() == cue.group()) groupCount++;
		}
		if (groupCount >= MAX_GROUP_OFFERS) {
			droppedOffers++;
			return new Admission(AdmissionResult.GROUP_LIMIT, groupCount);
		}
		if (burst.size() >= MAX_GLOBAL_OFFERS) {
			droppedOffers++;
			return new Admission(AdmissionResult.GLOBAL_LIMIT, groupCount);
		}
		burst.addLast(new BurstOffer(cue, cell, gameTime));
		admittedOffers++;
		return new Admission(AdmissionResult.ADMITTED, groupCount + 1);
	}

	public void reset() {
		remembered.clear();
		burst.clear();
		newestEventId = -1L;
		acceptedEvents = 0;
		duplicateEvents = 0;
		staleEvents = 0;
		admittedOffers = 0;
		coalescedOffers = 0;
		droppedOffers = 0;
	}

	public Metrics metrics() {
		long oldest = remembered.isEmpty() ? -1L : remembered.keySet().iterator().next();
		return new Metrics(acceptedEvents, duplicateEvents, staleEvents, admittedOffers,
				coalescedOffers, droppedOffers, remembered.size(), oldest, newestEventId);
	}

	private void prune(long gameTime) {
		while (!burst.isEmpty()) {
			long age = gameTime - burst.getFirst().gameTime();
			if (age < BURST_WINDOW_TICKS) break;
			burst.removeFirst();
		}
	}

	private static long floor(double value) {
		return (long) Math.floor(value);
	}

	public enum AdmissionResult { ADMITTED, COALESCED, GROUP_LIMIT, GLOBAL_LIMIT }

	public record Admission(AdmissionResult result, int concurrentInGroup) { }

	public record Metrics(long acceptedEvents, long duplicateEvents, long staleEvents,
			long admittedOffers, long coalescedOffers, long droppedOffers,
			int rememberedEvents, long oldestRememberedEventId, long newestEventId) {
		public static Metrics empty() {
			return new Metrics(0, 0, 0, 0, 0, 0, 0, -1, -1);
		}
	}

	private record OriginCell(long x, long y, long z) { }
	private record BurstOffer(LayeredAudioCue cue, OriginCell cell, long gameTime) { }
}
