package com.powers.magic.runtime;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MagicRayCollisionRulesTest {
	private static final UUID FIRST = UUID.fromString("00000000-0000-0000-0000-000000000001");
	private static final UUID SECOND = UUID.fromString("00000000-0000-0000-0000-000000000002");

	@Test
	void crossingSegmentsResolveAtTheirPhysicalIntersection() {
		var first = segment(FIRST, "energy_beam", new Vec3(-4, 2, 0), new Vec3(4, 2, 0), 100);
		var second = segment(SECOND, "void_beam", new Vec3(0, 2, -4), new Vec3(0, 2, 4), 100);

		Vec3 collision = MagicRayCollisionRules.intersection(first, second).orElseThrow();

		assertEquals(0.0, collision.x, 0.0001);
		assertEquals(2.0, collision.y, 0.0001);
		assertEquals(0.0, collision.z, 0.0001);
	}

	@Test
	void parallelDistantStaleAndSameOwnerSegmentsDoNotCollide() {
		var first = segment(FIRST, "energy_beam", Vec3.ZERO, new Vec3(8, 0, 0), 100);
		assertFalse(MagicRayCollisionRules.intersection(first,
				segment(SECOND, "void_beam", new Vec3(0, 2, 0), new Vec3(8, 2, 0), 100)).isPresent());
		assertFalse(MagicRayCollisionRules.mayCompare(first,
				segment(FIRST, "void_beam", new Vec3(4, -2, 0), new Vec3(4, 2, 0), 100), 100));
		assertFalse(MagicRayCollisionRules.mayCompare(first,
				segment(SECOND, "void_beam", new Vec3(4, -2, 0), new Vec3(4, 2, 0), 95), 100));
	}

	@Test
	void recentDistinctOwnersAndFiniteSegmentsAreComparable() {
		var first = segment(FIRST, "energy_beam", Vec3.ZERO, new Vec3(8, 0, 0), 100);
		var second = segment(SECOND, "void_beam", new Vec3(4, -2, 0), new Vec3(4, 2, 0), 98);

		assertTrue(MagicRayCollisionRules.mayCompare(first, second, 100));
		assertEquals(4, MagicRayCollisionRules.RETENTION_TICKS);
		assertEquals(32, MagicRayCollisionRules.MAX_COLLISIONS_PER_TICK);
	}

	@Test
	void aRayCounteredAtItsOriginIsAValidNoGeometryOutcome() {
		Vec3 origin = new Vec3(4.5, 65.0, -2.5);

		assertFalse(MagicRaySegment.hasUsableGeometry(origin, origin));
		assertFalse(MagicRaySegment.hasUsableGeometry(origin,
				new Vec3(Double.NaN, 65.0, -2.5)));
		assertTrue(MagicRaySegment.hasUsableGeometry(origin, origin.add(0.0, 0.0, 0.01)));
	}

	private static MagicRaySegment segment(UUID owner, String action, Vec3 start, Vec3 end, long tick) {
		return new MagicRaySegment(owner, action, "minecraft:overworld", start, end, tick);
	}
}
