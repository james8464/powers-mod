package com.powers.forge;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.powers.item.artifact.ArtifactAlignment;
import net.minecraft.resources.Identifier;

/** Validated persistent identity and progression for one Arcane Crucible weapon. */
public record CrucibleWeaponData(int schemaVersion, Identifier lineageId,
		ArtifactAlignment alignment, boolean starBound, long xp, int level) {
	public static final int SCHEMA_VERSION = 1;

	private record Serialized(int schemaVersion, Identifier lineageId,
			ArtifactAlignment alignment, boolean starBound, long xp, int level) {
	}

	private static final Codec<Serialized> SERIALIZED_CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.INT.fieldOf("schema_version").forGetter(Serialized::schemaVersion),
			Identifier.CODEC.fieldOf("lineage").forGetter(Serialized::lineageId),
			ArtifactAlignment.CODEC.fieldOf("alignment").forGetter(Serialized::alignment),
			Codec.BOOL.fieldOf("star_bound").forGetter(Serialized::starBound),
			Codec.LONG.fieldOf("xp").forGetter(Serialized::xp),
			Codec.INT.fieldOf("level").forGetter(Serialized::level))
			.apply(instance, Serialized::new));

	public static final Codec<CrucibleWeaponData> CODEC = SERIALIZED_CODEC.flatXmap(
			serialized -> serialized.schemaVersion() == SCHEMA_VERSION
					? DataResult.success(create(serialized.lineageId(), serialized.alignment(),
							serialized.starBound(), serialized.xp()))
					: DataResult.error(() -> "Unsupported Crucible weapon schema: "
							+ serialized.schemaVersion()),
			data -> DataResult.success(new Serialized(data.schemaVersion(), data.lineageId(),
					data.alignment(), data.starBound(), data.xp(), data.level())));

	public CrucibleWeaponData {
		if (schemaVersion != SCHEMA_VERSION) throw new IllegalArgumentException("Unsupported Crucible schema");
		if (lineageId == null || alignment == null) throw new IllegalArgumentException("Incomplete Crucible data");
		xp = Math.max(0L, xp);
		level = CrucibleXpRules.levelForXp(xp);
	}

	public static CrucibleWeaponData create(Identifier lineageId, ArtifactAlignment alignment,
			boolean starBound, long xp) {
		return new CrucibleWeaponData(SCHEMA_VERSION, lineageId, alignment, starBound, xp, 0);
	}

	public CrucibleWeaponData bindStar() {
		return starBound ? this : create(lineageId, alignment, true, xp);
	}

	public CrucibleWeaponData awardXp(long amount) {
		return create(lineageId, alignment, starBound, CrucibleXpRules.addSaturated(xp, amount));
	}
}
