package com.powers.forge;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CrucibleMutationGuardTest {
	@Test
	void onlyOneServerTransactionMayOwnTheInventory() {
		CrucibleMutationGuard guard = new CrucibleMutationGuard();
		assertTrue(guard.tryLock());
		assertFalse(guard.tryLock());
		guard.unlock();
		assertTrue(guard.tryLock());
		guard.unlock();
	}
}
