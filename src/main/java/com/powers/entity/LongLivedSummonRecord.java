package com.powers.entity;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/** The complete authoritative custom state of a finite guardian summon. */
public record LongLivedSummonRecord(
		UUID stableId,
		@Nullable UUID ownerId,
		Task task,
		Archetype archetype,
		long expiresAtGameTime
) {
	private static final String DATA_KEY = "PowersSummon";
	private static final int MAX_LIFETIME_TICKS = 72_000;

	public LongLivedSummonRecord {
		Objects.requireNonNull(stableId, "stableId");
		Objects.requireNonNull(task, "task");
		Objects.requireNonNull(archetype, "archetype");
		if ((ownerId != null) != (task == Task.GUARD)) {
			throw new IllegalArgumentException("GUARD requires one owner; INVADE requires none");
		}
	}

	/** Creates a record whose absolute expiry keeps advancing while its chunk is unloaded. */
	public static LongLivedSummonRecord create(UUID stableId, @Nullable UUID ownerId,
			Task task, Archetype archetype, long gameTime, int lifetimeTicks) {
		int boundedLifetime = Math.min(MAX_LIFETIME_TICKS, Math.max(1, lifetimeTicks));
		return new LongLivedSummonRecord(stableId, ownerId, task, archetype,
				saturatingAdd(gameTime, boundedLifetime));
	}

	/** Converts the former remaining-ticks representation without extending its upper bound. */
	public static LongLivedSummonRecord fromLegacy(UUID stableId, @Nullable UUID ownerId,
			int remainingTicks, boolean elite, long gameTime) {
		Task task = ownerId == null ? Task.INVADE : Task.GUARD;
		Archetype archetype = elite ? Archetype.ELITE : Archetype.NORMAL;
		if (remainingTicks <= 0) {
			return new LongLivedSummonRecord(stableId, ownerId, task, archetype, gameTime);
		}
		return create(stableId, ownerId, task, archetype, gameTime, remainingTicks);
	}

	public boolean expiredAt(long gameTime) {
		return gameTime >= expiresAtGameTime;
	}

	/** Writes only compact authoritative facts; vanilla already writes {@link #stableId()} as UUID. */
	public void write(ValueOutput output) {
		ValueOutput summon = output.child(DATA_KEY);
		summon.storeNullable("o", UUIDUtil.CODEC, ownerId);
		summon.putByte("t", task.id());
		summon.putByte("a", archetype.id());
		summon.putLong("e", expiresAtGameTime);
	}

	/** Reads the compact schema and clamps corrupt far-future expiries to the supported lifetime. */
	public static Optional<LongLivedSummonRecord> read(ValueInput input, UUID stableId,
			long gameTime) {
		return input.child(DATA_KEY).map(summon -> {
			UUID ownerId = summon.read("o", UUIDUtil.CODEC).orElse(null);
			Optional<Task> storedTask = Task.fromId(summon.getByteOr("t", Byte.MIN_VALUE));
			Optional<Archetype> storedArchetype = Archetype.fromId(
					summon.getByteOr("a", Byte.MIN_VALUE));
			Task task = storedTask.orElse(ownerId == null ? Task.INVADE : Task.GUARD);
			Archetype archetype = storedArchetype.orElse(Archetype.NORMAL);
			long storedExpiry = summon.getLongOr("e", gameTime);
			long latestExpiry = saturatingAdd(gameTime, MAX_LIFETIME_TICKS);
			long boundedExpiry = Math.min(storedExpiry, latestExpiry);
			if (storedTask.isEmpty() || storedArchetype.isEmpty()
					|| (ownerId != null) != (task == Task.GUARD)) {
				task = ownerId == null ? Task.INVADE : Task.GUARD;
				boundedExpiry = gameTime;
			}
			return new LongLivedSummonRecord(stableId, ownerId, task, archetype, boundedExpiry);
		});
	}

	private static long saturatingAdd(long left, long right) {
		if (right > 0 && left > Long.MAX_VALUE - right) return Long.MAX_VALUE;
		return left + right;
	}

	/** Persisted behavior role; live targets and navigation remain derived runtime state. */
	public enum Task {
		GUARD((byte) 0),
		INVADE((byte) 1);

		private final byte id;

		Task(byte id) {
			this.id = id;
		}

		public byte id() {
			return id;
		}

		public static Optional<Task> fromId(byte id) {
			if (id == GUARD.id) return Optional.of(GUARD);
			if (id == INVADE.id) return Optional.of(INVADE);
			return Optional.empty();
		}
	}

	/** Persisted statistics tier; alignment derives from the concrete entity type. */
	public enum Archetype {
		NORMAL((byte) 0),
		ELITE((byte) 1);

		private final byte id;

		Archetype(byte id) {
			this.id = id;
		}

		public byte id() {
			return id;
		}

		public static Optional<Archetype> fromId(byte id) {
			if (id == NORMAL.id) return Optional.of(NORMAL);
			if (id == ELITE.id) return Optional.of(ELITE);
			return Optional.empty();
		}
	}
}
