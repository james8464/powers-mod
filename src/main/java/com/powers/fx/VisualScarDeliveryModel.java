package com.powers.fx;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Defines immutable visual-scar delivery keys, sends, acknowledgements, and shared snapshots. */
public final class VisualScarDeliveryModel {
	private VisualScarDeliveryModel() { }

	public record ScarKey(long position, int face) implements Comparable<ScarKey> {
		public ScarKey {
			if (face < 0 || face >= 6) throw new IllegalArgumentException("invalid scar face");
		}
		@Override public int compareTo(ScarKey other) {
			int order = Long.compare(position, other.position);
			return order != 0 ? order : Integer.compare(face, other.face);
		}
	}

	public record SnapshotRow(String dimension, ScarFxProtocolRules.Wire wire) {
		public SnapshotRow {
			dimension = Objects.requireNonNull(dimension, "dimension");
			wire = Objects.requireNonNull(wire, "wire");
			if (dimension.isBlank() || !ScarFxProtocolRules.validate(wire)
					|| wire.operation() == ScarFxProtocolRules.RESET_DIMENSION) {
				throw new IllegalArgumentException("invalid snapshot row");
			}
		}
		public ScarKey key() { return new ScarKey(wire.position(), wire.face()); }
	}

	public record ResyncCursor(long revision, ScarKey afterKey) {
		public ResyncCursor {
			if (revision < -1) throw new IllegalArgumentException("invalid snapshot revision");
		}
	}

	public record Send(VisualScarLedgerRules.ObserverSession session,
			ScarFxProtocolRules.Wire payload, long deliveryGeneration, boolean resync) {
		public Send {
			session = Objects.requireNonNull(session, "session");
			payload = Objects.requireNonNull(payload, "payload");
			if (deliveryGeneration < 0 || resync && deliveryGeneration == 0) {
				throw new IllegalArgumentException("invalid delivery guard");
			}
	}
	}

	public record Drain(List<Send> sent, VisualScarDeliveryRules.Pending remaining,
			int staleEntriesDropped, int liveSent, int resyncSent) {
		public Drain { sent = List.copyOf(sent); }
	}

	public record Eviction(VisualScarLedgerRules.ObserverSession victimSession, ScarKey victimKey) {
		public Eviction {
			victimSession = Objects.requireNonNull(victimSession, "victimSession");
			victimKey = Objects.requireNonNull(victimKey, "victimKey");
		}
	}

	public record OfferResult(VisualScarDeliveryRules.Pending pending, Optional<Eviction> eviction) {
		public OfferResult {
			pending = Objects.requireNonNull(pending, "pending");
			eviction = Objects.requireNonNull(eviction, "eviction");
		}
	}

	/** Immutable shared snapshot; observers retain only a reference, revision, and stable key. */
	public static final class AuthoritativeSnapshot {
		private final long revision;
		private final List<SnapshotRow> rows;

		AuthoritativeSnapshot(long revision, List<SnapshotRow> rows) {
			if (revision < 0) throw new IllegalArgumentException("negative authoritative revision");
			Objects.requireNonNull(rows, "rows");
			if (rows.size() > 2_048) throw new IllegalArgumentException("authoritative snapshot exceeds cap");
			this.revision = revision;
			this.rows = rows.stream().sorted(java.util.Comparator
					.comparing(SnapshotRow::dimension).thenComparing(SnapshotRow::key)).toList();
			for (int index = 1; index < this.rows.size(); index++) {
				SnapshotRow previous = this.rows.get(index - 1);
				SnapshotRow current = this.rows.get(index);
				if (previous.dimension().equals(current.dimension())
						&& previous.key().equals(current.key())) {
					throw new IllegalArgumentException("duplicate authoritative scar key");
				}
			}
		}

		public long revision() { return revision; }
		public List<SnapshotRow> rows() { return rows; }
		public ResyncCursor cursor() { return new ResyncCursor(revision, null); }
		public Map<ScarKey, Long> generations(String dimension) {
			Objects.requireNonNull(dimension, "dimension");
			Map<ScarKey, Long> result = new LinkedHashMap<>();
			for (SnapshotRow row : rows) {
				if (row.dimension().equals(dimension)
						&& row.wire().operation() == ScarFxProtocolRules.CREATE_OR_UPDATE) {
					result.put(row.key(), row.wire().generation());
				}
			}
			return Map.copyOf(result);
		}

		Optional<SnapshotRow> next(String dimension, ResyncCursor cursor) {
			ScarKey after = cursor.revision() == revision ? cursor.afterKey() : null;
			for (SnapshotRow row : rows) {
				int dimensionOrder = row.dimension().compareTo(dimension);
				if (dimensionOrder < 0) continue;
				if (dimensionOrder > 0) return Optional.empty();
				if (after == null || row.key().compareTo(after) > 0) return Optional.of(row);
			}
			return Optional.empty();
		}

		Optional<SnapshotRow> lookup(String dimension, ScarKey key) {
			for (SnapshotRow row : rows) {
				int dimensionOrder = row.dimension().compareTo(dimension);
				if (dimensionOrder < 0) continue;
				if (dimensionOrder > 0) return Optional.empty();
				int keyOrder = row.key().compareTo(key);
				if (keyOrder == 0) return Optional.of(row);
				if (keyOrder > 0) return Optional.empty();
			}
			return Optional.empty();
		}
	}
}
