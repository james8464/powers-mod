package com.powers.power.state;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OwnedFreezeIndexTest {
	@Test
	void overlappingOwnersRestoreOnlyAfterTheLastRelease() {
		OwnedFreezeIndex index = new OwnedFreezeIndex();
		UUID entity = UUID.randomUUID();
		UUID first = UUID.randomUUID();
		UUID second = UUID.randomUUID();

		assertTrue(index.claim(entity, first));
		assertFalse(index.claim(entity, second));
		assertFalse(index.release(entity, first));
		assertTrue(index.isClaimed(entity));
		assertTrue(index.release(entity, second));
		assertFalse(index.isClaimed(entity));
	}
}
