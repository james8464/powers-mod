package com.powers.realm;

import com.powers.PowersEntities;
import com.powers.PowersSounds;
import com.powers.entity.RealmHerald;
import com.powers.fx.PowerFx;
import com.powers.util.BoundedEntityCandidates;
import com.powers.util.LoadedChunks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;

/** Spawns at most one persistent Herald in each completed, loaded realm court. */
public final class RealmHeraldManager {
	private RealmHeraldManager() {
	}

	public static void tick(ServerLevel level, RealmKind kind) {
		if (level.getGameTime() % 20 != 0) return;
		MemorySite court = RealmLayout.sites(kind).stream()
				.filter(site -> site.landmarkType() == RealmLandmarkType.HERALD_COURT)
				.findFirst().orElseThrow();
		RealmLandmarkSavedData landmarks = level.getServer().overworld().getDataStorage()
				.computeIfAbsent(RealmLandmarkSavedData.TYPE);
		String dimension = level.dimension().identifier().toString();
		if (!landmarks.missing(dimension, java.util.List.of(court.id())).isEmpty()) return;
		BlockPos center = new BlockPos(court.x(), level.getMinY() + 2, court.z());
		if (!LoadedChunks.contains(level, center)) return;
		var nearby = BoundedEntityCandidates.ofClass(level, RealmHerald.class,
				new net.minecraft.world.phys.AABB(center).inflate(96.0), 1, RealmHerald::isAlive);
		if (!nearby.isEmpty()) return;
		RealmHeraldSavedData heralds = level.getServer().overworld().getDataStorage()
				.computeIfAbsent(RealmHeraldSavedData.TYPE);
		if (!heralds.maySpawn(dimension, level.getGameTime())) return;
		RealmHerald herald = (kind == RealmKind.LIGHT
				? PowersEntities.LIGHT_HERALD : PowersEntities.DARK_HERALD)
				.create(level, EntitySpawnReason.EVENT);
		if (herald == null) return;
		herald.setPos(center.getX() + 0.5, center.getY(), center.getZ() + 0.5);
		herald.finalizeSpawn(level, level.getCurrentDifficultyAt(center),
				EntitySpawnReason.EVENT, null);
		if (!level.addFreshEntity(herald)) return;
		var lightning = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
		if (lightning != null) {
			lightning.setVisualOnly(true);
			lightning.setPos(herald.position());
			level.addFreshEntity(lightning);
		}
		int color = kind == RealmKind.LIGHT ? 0xFFF2A8 : 0x54206E;
		PowerFx.eventRune(level, herald.position(), 7.0, color, 64, 0.0);
		PowerFx.eventSpiral(level, herald.position(), 2.0, 9.0, color, 54, 0.0);
		PowerFx.sound(level, herald.position(), kind == RealmKind.LIGHT
				? PowersSounds.LIGHT_CHORUS : PowersSounds.DARK_WHISPER, 3.0F,
				kind == RealmKind.LIGHT ? 0.65F : 0.35F);
	}
}
