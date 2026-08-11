package com.powers.power.state;

import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MagicShieldManagerTest {
	@Test
	void shieldConsumesFiniteIntegrityAndCollapses() {
		MagicShieldManager manager = new MagicShieldManager();
		UUID owner = UUID.randomUUID();
		manager.raise(owner, 20.0f, 100);

		MagicShieldManager.Impact first = manager.absorb(owner, 7.0f, 10);
		MagicShieldManager.Impact second = manager.absorb(owner, 20.0f, 11);

		assertTrue(first.blocked());
		assertEquals(13.0f, first.integrity(), 0.001f);
		assertTrue(second.blocked());
		assertTrue(second.shattered());
		assertFalse(manager.active(owner, 11));
	}

	@Test
	void expiredUnknownAndInvalidImpactsCannotCreateImmortality() {
		MagicShieldManager manager = new MagicShieldManager();
		UUID owner = UUID.randomUUID();
		manager.raise(owner, 4.0f, 5);

		assertFalse(manager.absorb(owner, Float.NaN, 1).blocked());
		assertFalse(manager.active(owner, 5));
		assertFalse(manager.absorb(owner, 1.0f, 6).blocked());
		assertFalse(manager.absorb(UUID.randomUUID(), 10.0f, 1).blocked());
	}

	@Test
	void sharedShieldCarriesTheCastersReflectionVariant() {
		MagicShieldManager manager = new MagicShieldManager();
		UUID protectedPlayer = UUID.randomUUID();
		manager.raise(protectedPlayer, 20.0F, Long.MAX_VALUE, true);

		MagicShieldManager.Impact impact = manager.absorb(protectedPlayer, 3.0F, 20);

		assertTrue(impact.blocked());
		assertTrue(impact.reflective());
	}

	@Test
	void activeOwnerSnapshotIsBoundedByActualShieldsAndDropsExpiredEntries() {
		MagicShieldManager manager = new MagicShieldManager();
		UUID live = UUID.randomUUID();
		UUID expired = UUID.randomUUID();
		manager.raise(live, 20.0F, 100);
		manager.raise(expired, 20.0F, 5);

		assertEquals(Set.of(live), manager.activeOwners(5));
	}
}
