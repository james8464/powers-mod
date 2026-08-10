package com.powers.power.abilities;

import com.powers.magic.runtime.CastSource;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Identifier-only, server-thread state for one finite Faultbound Verdict. */
final class FaultboundVerdict {
	final UUID owner;
	final ResourceKey<Level> dimension;
	final CastSource castSource;
	final int terrainTier;
	final long startedAt;
	final long expiresAt;
	final Vec3 origin;
	final Vec3 lookDirection;
	final double baseRadius;
	final float baseDamage;
	final int mantleDuration;
	final boolean empoweredImpact;
	final boolean secondStep;
	final boolean trueSight;
	final boolean reflectiveWard;
	final boolean soulEcho;
	final boolean afterimage;
	final boolean ancientMastery;
	final Map<UUID, Integer> hits = new LinkedHashMap<>();
	Vec3 center;
	boolean primaryResolved;
	boolean echoResolved;
	boolean crownResolved;

	FaultboundVerdict(UUID owner, ResourceKey<Level> dimension, CastSource castSource, int terrainTier,
			long startedAt, long expiresAt, Vec3 origin, Vec3 lookDirection,
			double baseRadius, float baseDamage, int mantleDuration, boolean empoweredImpact,
			boolean secondStep, boolean trueSight, boolean reflectiveWard,
			boolean soulEcho, boolean afterimage, boolean ancientMastery) {
		this.owner = owner;
		this.dimension = dimension;
		this.castSource = castSource;
		this.terrainTier = Math.clamp(terrainTier, 0, 10);
		this.startedAt = startedAt;
		this.expiresAt = expiresAt;
		this.origin = origin;
		this.center = origin;
		this.lookDirection = lookDirection;
		this.baseRadius = baseRadius;
		this.baseDamage = baseDamage;
		this.mantleDuration = mantleDuration;
		this.empoweredImpact = empoweredImpact;
		this.secondStep = secondStep;
		this.trueSight = trueSight;
		this.reflectiveWard = reflectiveWard;
		this.soulEcho = soulEcho;
		this.afterimage = afterimage;
		this.ancientMastery = ancientMastery;
	}
}
