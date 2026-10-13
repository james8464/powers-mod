package com.powers.allegiance;

import com.google.gson.JsonObject;

import java.util.Objects;

/** Atomically publishes validated allegiance policy snapshots for live server consumers. */
public final class AllegianceRegistry {
	public record Snapshot(long revision, AllegianceMatrix matrix) {
		public Snapshot {
			if (revision < 0L) throw new IllegalArgumentException("revision");
			Objects.requireNonNull(matrix, "matrix");
		}
	}

	private volatile Snapshot snapshot = new Snapshot(0L, AllegianceMatrix.defaults());

	public Snapshot snapshot() {
		return snapshot;
	}

	/** Returns false without publication when the candidate document is incomplete or invalid. */
	public synchronized boolean reload(JsonObject candidate) {
		final AllegianceMatrix parsed;
		try {
			parsed = AllegianceMatrix.parse(candidate);
		} catch (RuntimeException invalid) {
			return false;
		}
		snapshot = new Snapshot(snapshot.revision() + 1L, parsed);
		return true;
	}
}
