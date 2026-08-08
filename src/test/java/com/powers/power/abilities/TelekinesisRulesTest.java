package com.powers.power.abilities;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TelekinesisRulesTest {
	@Test
	void flingMovesHorizontallyAwayFromTheCasterAtTheRequestedMagnitude() {
		Vec3 impulse = TelekinesisRules.outwardFling(
				Vec3.ZERO, new Vec3(3.0, 9.0, 4.0), 2.2, 0.7);

		assertEquals(1.32, impulse.x, 0.0001);
		assertEquals(0.70, impulse.y, 0.0001);
		assertEquals(1.76, impulse.z, 0.0001);
		assertEquals(2.2, impulse.horizontalDistance(), 0.0001);
	}

	@Test
	void flingPreservesNegativeDirectionAndRejectsUndefinedVectors() {
		Vec3 negative = TelekinesisRules.outwardFling(
				Vec3.ZERO, new Vec3(-3.0, 2.0, -4.0), 2.2, 0.7);
		assertTrue(negative.x < 0.0);
		assertTrue(negative.z < 0.0);
		assertEquals(Vec3.ZERO, TelekinesisRules.outwardFling(Vec3.ZERO, Vec3.ZERO, 2.2, 0.7));
		assertEquals(Vec3.ZERO, TelekinesisRules.outwardFling(
				Vec3.ZERO, new Vec3(1.0, 0.0, 0.0), Double.NaN, 0.7));
		assertEquals(Vec3.ZERO, TelekinesisRules.outwardFling(
				null, new Vec3(1.0, 0.0, 0.0), 2.2, 0.7));
		assertEquals(Vec3.ZERO, TelekinesisRules.outwardFling(
				Vec3.ZERO, new Vec3(1.0, 0.0, 0.0), -2.2, 0.7));
	}

	@Test
	void castResolvesOnlyWhenSomethingMovedOrReflected() {
		assertFalse(TelekinesisRules.resolved(0, 0));
		assertFalse(TelekinesisRules.resolved(-1, -1));
		assertTrue(TelekinesisRules.resolved(1, 0));
		assertTrue(TelekinesisRules.resolved(0, 1));
	}
}
