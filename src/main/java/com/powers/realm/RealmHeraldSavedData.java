package com.powers.realm;

import com.mojang.serialization.Codec;
import com.powers.PowersMod;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.Map;

/** Persists the next legal Herald manifestation time independently per realm. */
public final class RealmHeraldSavedData extends SavedData {
	public static final Codec<RealmHeraldSavedData> CODEC = Codec.unboundedMap(Codec.STRING, Codec.LONG)
			.optionalFieldOf("next_spawn", Map.of())
			.xmap(RealmHeraldSavedData::new, data -> Map.copyOf(data.nextSpawnAt)).codec();
	public static final SavedDataType<RealmHeraldSavedData> TYPE = new SavedDataType<>(
			PowersMod.id("realm_heralds"), RealmHeraldSavedData::new, CODEC,
			DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

	private final Map<String, Long> nextSpawnAt;

	public RealmHeraldSavedData() {
		this(Map.of());
	}

	private RealmHeraldSavedData(Map<String, Long> snapshot) {
		nextSpawnAt = new HashMap<>(snapshot);
	}

	public boolean maySpawn(String dimension, long gameTime) {
		return RealmHeraldRules.maySpawn(gameTime, nextSpawnAt.getOrDefault(dimension, 0L));
	}

	public void recordDefeat(String dimension, long gameTime) {
		nextSpawnAt.put(dimension, RealmHeraldRules.nextSpawnTime(gameTime));
		setDirty();
	}
}
