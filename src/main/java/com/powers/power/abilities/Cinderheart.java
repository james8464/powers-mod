package com.powers.power.abilities;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/** Identifier-only, server-thread state for one finite Cinderheart lifecycle. */
final class Cinderheart {
	final UUID originalOwner;
	final UUID projectile;
	final ResourceKey<Level> dimension;
	final long startedAt;
	UUID controller;
	long expiresAt;
	double potencyMultiplier;
	boolean empoweredImpact;
	boolean reflectiveWard;
	boolean afterimage;
	boolean trueSight;
	boolean ancientMastery;
	int tier = 1;
	int reflections;
	boolean launched;
	Vec3 lastPosition;

	Cinderheart(UUID originalOwner, UUID projectile,
			ResourceKey<Level> dimension, long startedAt, long expiresAt,
			double potencyMultiplier, boolean empoweredImpact, boolean reflectiveWard,
			boolean afterimage, boolean trueSight, boolean ancientMastery,
			Vec3 lastPosition) {
		this.originalOwner = originalOwner;
		this.projectile = projectile;
		this.dimension = dimension;
		this.startedAt = startedAt;
		this.controller = originalOwner;
		this.expiresAt = expiresAt;
		this.potencyMultiplier = potencyMultiplier;
		this.empoweredImpact = empoweredImpact;
		this.reflectiveWard = reflectiveWard;
		this.afterimage = afterimage;
		this.trueSight = trueSight;
		this.ancientMastery = ancientMastery;
		this.lastPosition = lastPosition;
	}
}
