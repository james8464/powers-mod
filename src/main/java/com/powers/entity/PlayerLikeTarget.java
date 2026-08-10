package com.powers.entity;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/** Marker and shared identity rules for safe player-compatible test targets. */
public interface PlayerLikeTarget {
	String testingUsername();

	static boolean isCompatible(Object target) {
		return target instanceof ServerPlayer || target instanceof PlayerLikeTarget;
	}

	static boolean alwaysConsents(Object target) {
		return target instanceof PlayerLikeTarget;
	}

	static String username(LivingEntity target) {
		return target instanceof PlayerLikeTarget testing
				? testing.testingUsername() : target.getName().getString();
	}
}
