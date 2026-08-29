package com.powers.power.state;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.powers.PowersMod;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** Crash journal proving that a persisted frozen clock belonged to POWERS. */
public final class TimeStopSavedData extends SavedData {
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
