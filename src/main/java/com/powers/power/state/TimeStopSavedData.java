package com.powers.power.state;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.powers.PowersMod;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.UUID;
import java.util.function.Consumer;

/** Crash journal proving that a persisted frozen clock belonged to POWERS. */
public final class TimeStopSavedData extends SavedData {
	public enum RecoveryDecision {
		NONE(false, false),
		CLEAR_ONLY(true, false),
		CLEAR_AND_UNFREEZE(true, true);

		private final boolean clearJournal;
		private final boolean unfreeze;

		RecoveryDecision(boolean clearJournal, boolean unfreeze) {
			this.clearJournal = clearJournal;
			this.unfreeze = unfreeze;
		}

		public boolean clearJournal() {
			return clearJournal;
		}

		public boolean unfreeze() {
			return unfreeze;
		}
	}

	public record Snapshot(int schemaVersion, boolean active, long leaseToken,
			String owner, String source, long acquiredControlTick, long deadline,
			String shadowBody) {
		private static final Codec<Snapshot> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.INT.optionalFieldOf("schema_version", 1).forGetter(Snapshot::schemaVersion),
				Codec.BOOL.optionalFieldOf("active", false).forGetter(Snapshot::active),
				Codec.LONG.optionalFieldOf("lease_token", 0L).forGetter(Snapshot::leaseToken),
				Codec.STRING.optionalFieldOf("owner", "").forGetter(Snapshot::owner),
				Codec.STRING.optionalFieldOf("source", "").forGetter(Snapshot::source),
				Codec.LONG.optionalFieldOf("acquired_control_tick", 0L)
						.forGetter(Snapshot::acquiredControlTick),
				Codec.LONG.optionalFieldOf("deadline", 0L).forGetter(Snapshot::deadline),
				Codec.STRING.optionalFieldOf("shadow_body", "").forGetter(Snapshot::shadowBody)
		).apply(instance, Snapshot::new));

		public boolean staleRecoveryOnly() {
			return active && (schemaVersion < 2 || leaseToken <= 0L);
		}

		/** A malformed journal may be cleared, but never proves authority to thaw vanilla. */
		public RecoveryDecision recoveryDecision() {
			if (!active) return RecoveryDecision.NONE;
			return validRecoveryIdentity() ? RecoveryDecision.CLEAR_AND_UNFREEZE
					: RecoveryDecision.CLEAR_ONLY;
		}

		private boolean validRecoveryIdentity() {
			if (schemaVersion != 1 && schemaVersion != 2) return false;
			if (schemaVersion == 1 && (leaseToken != 0L || acquiredControlTick != 0L)) return false;
			if (schemaVersion == 2 && (leaseToken <= 0L || acquiredControlTick < 0L)) return false;
			if (!validUuid(owner)) return false;
			TimeStopLeaseSource parsedSource;
			try {
				parsedSource = TimeStopLeaseSource.valueOf(source);
			} catch (IllegalArgumentException | NullPointerException ignored) {
				return false;
			}
			long acquired = schemaVersion == 1 ? 0L : acquiredControlTick;
			if (deadline < acquired) return false;
			if (schemaVersion == 1) {
				// Schema 1 had no acquisition tick or typed deadline semantics. A non-negative
				// value is the strongest legacy validation available.
			} else if (parsedSource == TimeStopLeaseSource.CRYSTAL) {
				if (deadline == Long.MAX_VALUE || deadline <= acquired) return false;
			} else if (deadline != Long.MAX_VALUE) {
				return false;
			}
			return parsedSource == TimeStopLeaseSource.SHADOW
					? validUuid(shadowBody) : shadowBody != null && shadowBody.isEmpty();
		}

		private static boolean validUuid(String value) {
			if (value == null || value.isEmpty()) return false;
			try {
				UUID.fromString(value);
				return true;
			} catch (IllegalArgumentException ignored) {
				return false;
			}
		}
	}

	private static final Snapshot EMPTY = new Snapshot(2, false, 0L,
			"", "", 0L, 0L, "");
	public static final Codec<TimeStopSavedData> CODEC = Snapshot.CODEC
			.xmap(TimeStopSavedData::new, TimeStopSavedData::snapshot);
	public static final SavedDataType<TimeStopSavedData> TYPE = new SavedDataType<>(
			PowersMod.id("time_stop_ownership"), TimeStopSavedData::new, CODEC,
			DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

	private Snapshot snapshot;

	public TimeStopSavedData() {
		this(EMPTY);
	}

	private TimeStopSavedData(Snapshot snapshot) {
		this.snapshot = snapshot == null ? EMPTY : snapshot;
	}

	public Snapshot snapshot() {
		return snapshot;
	}

	public void activate(String owner, String source, long deadline, String shadowBody) {
		Snapshot replacement = new Snapshot(1, true, 0L, safe(owner), safe(source),
				0L, Math.max(0L, deadline), safe(shadowBody));
		replace(replacement);
	}

	public void activate(TimeStopLease lease) {
		Snapshot replacement = new Snapshot(2, true, lease.token(), lease.owner().toString(),
				lease.source().name(), lease.acquiredAt().value(), lease.deadline().value(),
				lease.shadowBody() == null ? "" : lease.shadowBody().toString());
		replace(replacement);
	}

	boolean activateAndSave(TimeStopLease lease, Runnable save) {
		return activateAndSave(lease, save, ignored -> { });
	}

	boolean activateAndSave(TimeStopLease lease, Runnable save,
			Consumer<RuntimeException> failureHandler) {
		Snapshot previous = snapshot;
		try {
			activate(lease);
			save.run();
			return true;
		} catch (RuntimeException failure) {
			replace(previous);
			failureHandler.accept(failure);
			return false;
		}
	}

	private void replace(Snapshot replacement) {
		if (replacement.equals(snapshot)) return;
		snapshot = replacement;
		setDirty();
	}

	public void clear() {
		if (EMPTY.equals(snapshot)) return;
		snapshot = EMPTY;
		setDirty();
	}

	private static String safe(String value) {
		return value == null ? "" : value;
	}
}
