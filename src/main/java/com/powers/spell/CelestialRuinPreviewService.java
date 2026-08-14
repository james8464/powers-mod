package com.powers.spell;

import com.powers.config.PowersConfig;
import com.powers.config.PowersConfigLoader;
import com.powers.config.ResolvedPowerPolicy;
import com.powers.protection.PowerProtection;
import com.powers.util.BoundedEntityCandidates;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

/** Builds read-only, bounded Heavenfall staging reports without loading chunks. */
public final class CelestialRuinPreviewService {
	private CelestialRuinPreviewService() {
	}

	/** Inspects loaded entities, configured safe zones, borders, and destruction policy. */
	public static CelestialRuinPreview preview(ServerLevel level, BlockPos center) {
		Vec3 epicenter = Vec3.atCenterOf(center);
		List<LivingEntity> candidates = BoundedEntityCandidates.living(level,
				CelestialRuinRules.damageBounds(epicenter, level.getMinY(), level.getMaxY()),
				CelestialRuinRules.ENTITY_LIMIT, Entity::isAlive,
				Comparator.comparingDouble(entity -> entity.distanceToSqr(epicenter)));
		String dimension = level.dimension().identifier().toString();
		int protectedRegions = 0;
		for (PowersConfig.SafeZone zone : PowersConfigLoader.get().safeZones()) {
			if (!dimension.equals(zone.dimension())) continue;
			double dx = zone.x() - epicenter.x;
			double dz = zone.z() - epicenter.z;
			double reach = zone.radius() + CelestialRuinRules.DAMAGE_RADIUS;
			if (dx * dx + dz * dz <= reach * reach) protectedRegions++;
		}
		ResolvedPowerPolicy policy = ResolvedPowerPolicy.resolve(level);
		return new CelestialRuinPreview(dimension, center.immutable(),
				CelestialRuinStagingRules.squareChunkFootprint(CelestialRuinRules.BLAST_RADIUS),
				CelestialRuinStagingRules.squareChunkFootprint(CelestialRuinRules.DAMAGE_RADIUS),
				candidates.size(), candidates.size() >= CelestialRuinRules.ENTITY_LIMIT,
				protectedRegions, policy.celestialRuinTerrainDamage(),
				policy.celestialRuinBlockEntityDamage(),
				level.getWorldBorder().isWithinBounds(center)
						&& !PowerProtection.isSafeZone(level, epicenter));
	}
}
