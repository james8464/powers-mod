package com.powers.power.abilities;

import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import com.powers.magic.runtime.CastSource;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Identifier-only, server-thread state for one finite Storm Tribunal. */
final class StormTribunal {
	final UUID owner;
	final int ownerEntityId;
	final Identifier sourcePower;
	final CastSource castSource;
	final ResourceKey<Level> dimension;
	final long startedAt;
	final long expiresAt;
	final Vec3 origin;
	final UUID trackedTarget;
	final double baseRadius;
	final float baseDamage;
	final boolean empoweredImpact;
	final boolean secondStep;
	final boolean trueSight;
	final boolean reflectiveWard;
	final boolean soulEcho;
	final boolean afterimage;
	final boolean ancientMastery;
	final Map<UUID, Integer> hits = new LinkedHashMap<>();
	final Set<UUID> groundedProjectiles = new LinkedHashSet<>();
	Vec3 center;
	boolean primaryResolved;
	boolean crownResolved;
	boolean afterimageSpent;

	StormTribunal(UUID owner, int ownerEntityId, Identifier sourcePower, CastSource castSource,
			ResourceKey<Level> dimension,
			long startedAt, long expiresAt, Vec3 origin, UUID trackedTarget,
			double baseRadius, float baseDamage, boolean empoweredImpact,
			boolean secondStep, boolean trueSight, boolean reflectiveWard,
			boolean soulEcho, boolean afterimage, boolean ancientMastery) {
		this.owner = owner;
		this.ownerEntityId = ownerEntityId;
		this.sourcePower = Objects.requireNonNull(sourcePower, "sourcePower");
		this.castSource = Objects.requireNonNull(castSource, "castSource");
		this.dimension = dimension;
		this.startedAt = startedAt;
		this.expiresAt = expiresAt;
		this.origin = origin;
		this.center = origin;
		this.trackedTarget = trackedTarget;
		this.baseRadius = baseRadius;
		this.baseDamage = baseDamage;
		this.empoweredImpact = empoweredImpact;
		this.secondStep = secondStep;
		this.trueSight = trueSight;
		this.reflectiveWard = reflectiveWard;
		this.soulEcho = soulEcho;
		this.afterimage = afterimage;
		this.ancientMastery = ancientMastery;
	}
}
