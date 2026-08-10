package com.powers.companion;

import net.minecraft.world.phys.Vec3;

import java.util.Set;
import java.util.UUID;

/** Pure privacy, eligibility, following, and authenticated-interaction rules. */
public final class PrivateCompanionRules {
	private PrivateCompanionRules() {
	}

	public static boolean eligible(boolean darknessTagged, boolean carriesShadowSword,
			boolean alive, boolean bodySession, boolean explicitlyRequested) {
		return darknessTagged && carriesShadowSword && alive && !bodySession && explicitlyRequested;
	}

	public static Vec3 followPoint(Vec3 owner, Vec3 look) {
		Vec3 horizontal = new Vec3(look.x, 0.0, look.z);
		if (horizontal.lengthSqr() < 1.0E-6) horizontal = new Vec3(0.0, 0.0, 1.0);
		return owner.subtract(horizontal.normalize().scale(3.5));
	}

	public static boolean shouldTeleport(Vec3 current, Vec3 desired) {
		return current.distanceToSqr(desired) > 20.0 * 20.0;
	}

	public static boolean mayInteract(long suppliedSession, long activeSession,
			double distanceSquared, double viewDot) {
		return suppliedSession == activeSession && distanceSquared <= 8.0 * 8.0 && viewDot >= 0.65;
	}

	/** Documents and tests the hard single-recipient privacy boundary. */
	public static Set<UUID> recipients(UUID owner) {
		return Set.of(owner);
	}
}
