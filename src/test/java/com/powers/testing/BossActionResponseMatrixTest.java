package com.powers.testing;

import com.powers.power.PowerRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class BossActionResponseMatrixTest {
	@Test
	void everyRegisteredPlayerActionHasAnExplicitResponseForEveryBoss() {
		PowerRegistry.initialize();
		var matrix = BossActionResponseMatrix.generated();
		assertEquals(PowerRegistry.getAll().size() * BossActionResponseMatrix.Boss.values().length,
				matrix.size());
		assertFalse(matrix.stream().anyMatch(entry -> entry.response()
				== BossActionResponseMatrix.Response.UNCLASSIFIED));
	}
}
