package com.powers.realm;

import com.mojang.serialization.Codec;
import com.powers.PowersMod;
import net.minecraft.core.UUIDUtil;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Durable outpost ownership: an unloaded Archivist is not an absent Archivist. */
public final class ArchivistSavedData extends SavedData {
	public static final Codec<ArchivistSavedData> CODEC = Codec.unboundedMap(Codec.STRING, UUIDUtil.CODEC)
			.optionalFieldOf("archivists", Map.of())
			.xmap(ArchivistSavedData::new, data -> Map.copyOf(data.owners)).codec();
	public static final SavedDataType<ArchivistSavedData> TYPE = new SavedDataType<>(
			PowersMod.id("middleworld_archivists"), ArchivistSavedData::new, CODEC,
			DataFixTypes.SAVED_DATA_COMMAND_STORAGE);
	private final Map<String, UUID> owners;

	public ArchivistSavedData() {
		this(Map.of());
	}

	private ArchivistSavedData(Map<String, UUID> snapshot) {
		owners = new HashMap<>(snapshot);
	}

	/** Ownership remains occupied even when the entity is not currently loaded. */
	public boolean occupied(String outpost) {
		return owners.containsKey(outpost);
	}

	/** First successful spawn/load owns the site; later loads cannot steal it. */
	public void claim(String outpost, UUID entity) {
		if (owners.putIfAbsent(outpost, entity) == null) setDirty();
	}

	/** Only destruction of the recorded owner releases its site. */
	public void release(String outpost, UUID entity) {
		if (owners.remove(outpost, entity)) setDirty();
	}
}
