package com.powers.fx;

import java.util.Objects;

/** Pure monotonic handler ownership and per-connection replay watermark. */
public record RankTenSilhouetteClientOwnership(long connectionEpoch, long dimensionGeneration,
		String dimension, long latestAcceptedEventId) {
	public RankTenSilhouetteClientOwnership {
		if (connectionEpoch < 0 || dimensionGeneration < 1 || !validDimension(dimension)
				|| latestAcceptedEventId < 0) {
			throw new IllegalArgumentException("invalid silhouette client ownership");
		}
	}

	/** Creates the first dimension generation for one connection. */
	public static RankTenSilhouetteClientOwnership empty(long connectionEpoch, String dimension) {
		return new RankTenSilhouetteClientOwnership(connectionEpoch, 1, dimension, 0);
	}

	/** Advances the generation whenever the observed world changes, including A to B to A. */
	public RankTenSilhouetteClientOwnership observeDimension(String observedDimension) {
		if (!validDimension(observedDimension)) {
			throw new IllegalArgumentException("invalid observed dimension");
		}
		if (dimension.equals(observedDimension)) return this;
		return advanceWorld(observedDimension);
	}

	/** Advances ownership for a replacement world even when its dimension identifier is unchanged. */
	public RankTenSilhouetteClientOwnership advanceWorld(String observedDimension) {
		if (!validDimension(observedDimension)) {
			throw new IllegalArgumentException("invalid observed dimension");
		}
		if (dimensionGeneration < Long.MAX_VALUE) {
			return new RankTenSilhouetteClientOwnership(connectionEpoch, dimensionGeneration + 1,
					observedDimension, latestAcceptedEventId);
		}
		long nextEpoch = connectionEpoch == Long.MAX_VALUE ? 1 : connectionEpoch + 1;
		return new RankTenSilhouetteClientOwnership(nextEpoch, 1, observedDimension,
				latestAcceptedEventId);
	}

	/** Returns the exact immutable owner captured before client-thread enqueue. */
	public HandlerStamp stamp() {
		return new HandlerStamp(connectionEpoch, dimensionGeneration, dimension);
	}

	/** Requires exact owner identity and a strictly newer event than any accepted this connection. */
	public boolean canAccept(HandlerStamp captured, long eventId) {
		return stamp().equals(captured) && eventId > latestAcceptedEventId;
	}

	/** Records a replay watermark only after the semantic state confirms insertion. */
	public RankTenSilhouetteClientOwnership accept(HandlerStamp captured, long eventId) {
		if (!canAccept(captured, eventId)) return this;
		return new RankTenSilhouetteClientOwnership(connectionEpoch, dimensionGeneration,
				dimension, eventId);
	}

	/** Starts a new connection; only a connection boundary may clear the replay watermark. */
	public RankTenSilhouetteClientOwnership resetConnection(long newConnectionEpoch,
			String initialDimension) {
		return empty(newConnectionEpoch, initialDimension);
	}

	private static boolean validDimension(String value) {
		return value != null && !value.isBlank() && value.length() <= 128;
	}

	/** Captured epoch, monotonically advancing world generation, and dimension identity. */
	public record HandlerStamp(long connectionEpoch, long dimensionGeneration, String dimension) {
		public HandlerStamp {
			if (connectionEpoch < 0 || dimensionGeneration < 1 || !validDimension(dimension)) {
				throw new IllegalArgumentException("invalid silhouette handler stamp");
			}
			dimension = Objects.requireNonNull(dimension, "dimension");
		}
	}
}
