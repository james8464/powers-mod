package com.powers.realm;

import com.mojang.serialization.Codec;
import com.powers.PowersMod;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.List;

/** World-owned per-site landmark completion, avoiding repeated realm rewrites after restarts. */
public final class RealmLandmarkSavedData extends SavedData {
	public static final Codec<RealmLandmarkSavedData> CODEC = Codec.STRING.listOf()
			.optionalFieldOf("completed_sites", List.of())
			.xmap(RealmLandmarkSavedData::new, data -> data.progress.snapshot()).codec();
	public static final SavedDataType<RealmLandmarkSavedData> TYPE = new SavedDataType<>(
			PowersMod.id("realm_landmarks"), RealmLandmarkSavedData::new, CODEC,
			DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

	private final RealmLandmarkProgress progress;

	public RealmLandmarkSavedData() {
		this(List.of());
	}

	private RealmLandmarkSavedData(List<String> snapshot) {
		progress = new RealmLandmarkProgress(snapshot);
	}

	public List<String> missing(String dimension, List<String> sites) {
		return progress.missing(dimension, sites);
	}

	public void complete(String dimension, String site) {
		if (progress.complete(dimension, site)) setDirty();
	}

	public int completedCount() {
		return progress.completedCount();
	}
}
