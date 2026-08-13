package com.powers.magic.runtime;

import com.powers.magic.MagicActionId;
import com.powers.magic.MagicDelivery;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.UUID;

/** Server adapter that validates, indexes, resolves, and presents live ray collisions. */
public final class MagicRayCollisionRuntime {
	private static final MagicRayCollisionIndex INDEX = new MagicRayCollisionIndex();

	private MagicRayCollisionRuntime() {
	}

	/** Publishes trusted server geometry and returns the first collision point, if any. */
	public static Optional<Vec3> publish(ServerLevel level, String action, UUID owner,
			Vec3 start, Vec3 end, long gameTime) {
		var definition = MagicRuntime.catalogue().definition(new MagicActionId(action));
		if (definition == null || definition.delivery() != MagicDelivery.BEAM) {
			throw new IllegalArgumentException("Only registered beam actions may publish ray geometry");
		}
		if (!MagicRaySegment.hasUsableGeometry(start, end)) return Optional.empty();
		MagicRaySegment segment = new MagicRaySegment(owner, action,
				level.dimension().identifier().toString(), start, end, gameTime);
		Optional<MagicRayCollisionIndex.Collision> collision = INDEX.submit(segment);
		collision.ifPresent(value -> ServerMagicCasts.emitPhysicalRayReaction(level, value,
				MagicRuntime.global().resolveInteraction(value.submitted().action(),
						value.existing().action())));
		if (collision.isPresent()) return collision.map(MagicRayCollisionIndex.Collision::point);
		return PhysicalMagicPresences.collideRayWithFields(level, action, owner, start, end, gameTime);
	}

	public static void tick(long gameTime) {
		INDEX.tick(gameTime);
	}

	public static void clear(UUID owner) {
		INDEX.clearOwner(owner);
	}

	public static int activeSegmentCount() {
		return INDEX.activeSegmentCount();
	}

	public static int collisionsThisTick() {
		return INDEX.collisionsThisTick();
	}

	public static void clearAll() {
		INDEX.clear();
	}
}
