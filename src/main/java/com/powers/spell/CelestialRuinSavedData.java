package com.powers.spell;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.powers.PowersMod;
import com.powers.util.BoundedSphereCursor;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.List;

/** World-owned persistence for Heavenfall countdowns and partial ruin waves. */
public final class CelestialRuinSavedData extends SavedData {
	private static final Codec<BoundedSphereCursor.Snapshot> CURSOR_CODEC = RecordCodecBuilder.create(instance ->
			instance.group(
					Codec.INT.fieldOf("radius").forGetter(BoundedSphereCursor.Snapshot::radius),
					Codec.INT.fieldOf("x").forGetter(BoundedSphereCursor.Snapshot::x),
					Codec.INT.fieldOf("y").forGetter(BoundedSphereCursor.Snapshot::y),
					Codec.INT.fieldOf("z").forGetter(BoundedSphereCursor.Snapshot::z),
					Codec.BOOL.fieldOf("finished").forGetter(BoundedSphereCursor.Snapshot::finished)
			).apply(instance, BoundedSphereCursor.Snapshot::new));

	public record Snapshot(String dimension, int x, int y, int z, String caster,
			int countdownRemaining, boolean detonated, BoundedSphereCursor.Snapshot cursor,
			int aftershockStep, String pendingPhase,
			BoundedSphereCursor.Snapshot pendingCursor, int pendingAftershockEnd) {
		public Snapshot(String dimension, int x, int y, int z, String caster,
				int countdownRemaining, boolean detonated, BoundedSphereCursor.Snapshot cursor) {
			this(dimension, x, y, z, caster, countdownRemaining, detonated, cursor, 0,
					"", cursor, 0);
		}

		public Snapshot(String dimension, int x, int y, int z, String caster,
				int countdownRemaining, boolean detonated, BoundedSphereCursor.Snapshot cursor,
				int aftershockStep) {
			this(dimension, x, y, z, caster, countdownRemaining, detonated, cursor,
					aftershockStep, "", cursor, aftershockStep);
		}
	}

	private static final Codec<Snapshot> SNAPSHOT_CODEC = RecordCodecBuilder.create(instance ->
				instance.group(
						Codec.STRING.fieldOf("dimension").forGetter(Snapshot::dimension),
						Codec.INT.fieldOf("x").forGetter(Snapshot::x),
						Codec.INT.fieldOf("y").forGetter(Snapshot::y),
						Codec.INT.fieldOf("z").forGetter(Snapshot::z),
						Codec.STRING.fieldOf("caster").forGetter(Snapshot::caster),
						Codec.INT.fieldOf("countdown_remaining").forGetter(Snapshot::countdownRemaining),
						Codec.BOOL.fieldOf("detonated").forGetter(Snapshot::detonated),
						CURSOR_CODEC.fieldOf("cursor").forGetter(Snapshot::cursor),
						Codec.INT.optionalFieldOf("aftershock_step", 0).forGetter(Snapshot::aftershockStep),
						Codec.STRING.optionalFieldOf("pending_phase", "").forGetter(Snapshot::pendingPhase),
						CURSOR_CODEC.optionalFieldOf("pending_cursor",
								new BoundedSphereCursor.Snapshot(0, 0, 0, 0, true))
								.forGetter(Snapshot::pendingCursor),
						Codec.INT.optionalFieldOf("pending_aftershock_end", 0)
								.forGetter(Snapshot::pendingAftershockEnd)
				).apply(instance, Snapshot::new));

	public static final Codec<CelestialRuinSavedData> CODEC = SNAPSHOT_CODEC.listOf()
			.optionalFieldOf("rituals", List.of())
			.xmap(CelestialRuinSavedData::new, CelestialRuinSavedData::snapshots).codec();
	public static final SavedDataType<CelestialRuinSavedData> TYPE = new SavedDataType<>(
			PowersMod.id("celestial_ruin"), CelestialRuinSavedData::new, CODEC,
			DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

	private List<Snapshot> snapshots;

	public CelestialRuinSavedData() {
		this(List.of());
	}

	CelestialRuinSavedData(List<Snapshot> snapshots) {
		this.snapshots = List.copyOf(snapshots);
	}

	public List<Snapshot> snapshots() {
		return snapshots;
	}

	public void replace(List<Snapshot> replacement) {
		List<Snapshot> immutable = List.copyOf(replacement);
		if (snapshots.equals(immutable)) return;
		snapshots = immutable;
		setDirty();
	}
}
