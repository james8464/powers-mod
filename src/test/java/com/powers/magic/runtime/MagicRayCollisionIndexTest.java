package com.powers.magic.runtime;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MagicRayCollisionIndexTest {
	@Test
	void secondCrossingRayProducesOneCollisionThenPairCooldownSuppressesSpam() {
		MagicRayCollisionIndex index = new MagicRayCollisionIndex();
		UUID sun = UUID.fromString("00000000-0000-0000-0000-000000000001");
		UUID voidCaster = UUID.fromString("00000000-0000-0000-0000-000000000002");

		assertFalse(index.submit(ray(sun, "energy_beam", -4, 0, 4, 0, 20)).isPresent());
		assertTrue(index.submit(ray(voidCaster, "void_beam", 0, -4, 0, 4, 20)).isPresent());
		assertFalse(index.submit(ray(voidCaster, "void_beam", 0, -4, 0, 4, 21)).isPresent());
		assertEquals(3, index.activeSegmentCount());
	}

	@Test
	void expiryAndOwnerCleanupRemoveOnlyRelevantBoundedHistory() {
		MagicRayCollisionIndex index = new MagicRayCollisionIndex();
		UUID first = UUID.fromString("00000000-0000-0000-0000-000000000001");
		UUID second = UUID.fromString("00000000-0000-0000-0000-000000000002");
		index.submit(ray(first, "energy_beam", -4, 0, 4, 0, 10));
		index.submit(ray(second, "void_beam", 0, -4, 0, 4, 10));

		index.tick(14);
		assertEquals(0, index.activeSegmentCount());
		index.submit(ray(first, "energy_beam", -4, 0, 4, 0, 15));
		index.submit(ray(second, "void_beam", 0, -4, 0, 4, 15));
		index.clearOwner(first);
		assertEquals(1, index.activeSegmentCount());
	}

	@Test
	void dimensionHistoryNeverExceedsItsFixedWorkBound() {
		MagicRayCollisionIndex index = new MagicRayCollisionIndex();
		UUID owner = UUID.fromString("00000000-0000-0000-0000-000000000001");
		for (int segment = 0; segment < MagicRayCollisionRules.MAX_SEGMENTS_PER_DIMENSION + 80; segment++) {
			index.submit(ray(owner, "energy_beam", segment, 0, segment + 1, 0, 40));
		}

		assertEquals(MagicRayCollisionRules.MAX_SEGMENTS_PER_DIMENSION,
				index.activeSegmentCount());
	}

	@Test
	void collisionWorkStopsAtThePerTickBudget() {
		MagicRayCollisionIndex index = new MagicRayCollisionIndex();
		for (int collision = 0; collision < MagicRayCollisionRules.MAX_COLLISIONS_PER_TICK + 8; collision++) {
			UUID sun = new UUID(0L, collision * 2L + 1L);
			UUID voidCaster = new UUID(0L, collision * 2L + 2L);
			index.submit(ray(sun, "energy_beam", -4, collision * 3.0, 4, collision * 3.0, 60));
			index.submit(ray(voidCaster, "void_beam", 0, collision * 3.0 - 2,
					0, collision * 3.0 + 2, 60));
		}

		assertEquals(MagicRayCollisionRules.MAX_COLLISIONS_PER_TICK,
				index.collisionsThisTick());
	}

	@Test
	void oneOwnerCannotConsumeTheWholeCollisionBudget() {
		MagicRayCollisionIndex index = new MagicRayCollisionIndex();
		UUID loud = new UUID(0L, 1L);
		for (int collision = 0; collision < 12; collision++) {
			UUID other = new UUID(1L, collision + 2L);
			double offset = collision * 5.0;
			index.submit(ray(other, "energy_beam", -4, offset, 4, offset, 70));
			index.submit(ray(loud, "void_beam", 0, offset - 2, 0, offset + 2, 70));
		}

		assertEquals(MagicRayCollisionRules.MAX_COLLISIONS_PER_OWNER_PER_TICK,
				index.collisionsThisTick());
	}

	private static MagicRaySegment ray(UUID owner, String action,
			double startX, double startZ, double endX, double endZ, long tick) {
		return new MagicRaySegment(owner, action, "minecraft:overworld",
				new Vec3(startX, 2, startZ), new Vec3(endX, 2, endZ), tick);
	}
}
