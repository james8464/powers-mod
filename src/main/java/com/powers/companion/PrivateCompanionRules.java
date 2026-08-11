package com.powers.companion;

import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
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
		Vec3 forward = horizontal.normalize();
		Vec3 left = new Vec3(forward.z, 0.0, -forward.x);
		return owner.subtract(forward.scale(2.75)).add(left.scale(1.75));
	}

	public static boolean shouldTeleport(Vec3 current, Vec3 desired) {
		return current.distanceToSqr(desired) > 12.0 * 12.0;
	}

	public static boolean mayInteract(long suppliedSession, long activeSession,
			double distanceSquared, double viewDot) {
		return suppliedSession == activeSession && distanceSquared <= 8.0 * 8.0 && viewDot >= 0.65;
	}

	/** Preserves server player order while enforcing owner-only hidden replies. */
	public static List<UUID> recipients(UUID owner, List<UUID> online, boolean revealed) {
		if (!revealed) return List.of(owner);
		List<UUID> recipients = new ArrayList<>(online.size() + 1);
		if (!online.contains(owner)) recipients.add(owner);
		recipients.addAll(online);
		return List.copyOf(recipients);
	}
}
