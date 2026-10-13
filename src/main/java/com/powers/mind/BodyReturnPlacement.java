package com.powers.mind;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import com.powers.power.travel.SafeDestinationResolver;
import com.powers.power.travel.TravelKind;

/** Bounded collision-safe return search over already available destination state. */
final class BodyReturnPlacement {
	private BodyReturnPlacement() { }

	static Vec3 findReturnSpot(ServerPlayer player, ServerLevel target, Vec3 requested,
			TravelKind travelKind) {
		for (int dy = 0; dy <= 3; dy++) {
			for (int radius = 0; radius <= 2; radius++) {
				for (int dx = -radius; dx <= radius; dx++) {
					for (int dz = -radius; dz <= radius; dz++) {
						Vec3 candidate = requested.add(dx, dy, dz);
						if (SafeDestinationResolver.validate(player, target, candidate,
								travelKind).allowed()) {
							return candidate;
						}
					}
				}
			}
		}
		return null;
	}

}
