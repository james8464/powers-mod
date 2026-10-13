package com.powers.realm;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.Villager;

/** Retains one world-owned identity through movement, dimension changes, and chunk unloads. */
public final class ArchivistLifecycle {
	static final String TAG_PREFIX = "powers_middleworld_archivist_";

	private ArchivistLifecycle() { }

	static ArchivistSavedData data(ServerLevel level) {
		return level.getServer().overworld().getDataStorage().computeIfAbsent(ArchivistSavedData.TYPE);
	}

	/** Adopts persistent tagged Archivists, including those from pre-identity saves. */
	public static void loaded(Entity entity, ServerLevel level) {
		if (!(entity instanceof Villager)) return;
		for (String tag : entity.entityTags()) {
			if (tag.startsWith(TAG_PREFIX)) data(level).claim(tag.substring(TAG_PREFIX.length()), entity.getUUID());
		}
	}

	/** Unload and dimension transfer preserve claims; killed/discarded owners release them. */
	public static void unloaded(Entity entity, ServerLevel level) {
		if (!(entity instanceof Villager) || entity.getRemovalReason() == null
				|| !entity.getRemovalReason().shouldDestroy()) return;
		for (String tag : entity.entityTags()) {
			if (tag.startsWith(TAG_PREFIX)) data(level).release(tag.substring(TAG_PREFIX.length()), entity.getUUID());
		}
	}
}
