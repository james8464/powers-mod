package com.powers.ai;

import net.minecraft.world.phys.Vec3;

import java.util.Objects;
import java.util.UUID;

/** Immutable, capability-neutral facts about one loaded living entity. */
public record PerceptionObservation(UUID entityId, Vec3 position, Vec3 eyePosition, boolean alive,
		boolean darknessAligned, boolean monster, boolean rangedAttack,
		double maximumHealth, double attackDamage) {
	public PerceptionObservation {
		Objects.requireNonNull(entityId, "entityId");
		Objects.requireNonNull(position, "position");
		Objects.requireNonNull(eyePosition, "eyePosition");
		maximumHealth = Math.max(0.0, maximumHealth);
		attackDamage = Math.max(0.0, attackDamage);
	}
}
