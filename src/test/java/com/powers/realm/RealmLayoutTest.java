package com.powers.realm;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RealmLayoutTest {
	@Test
	void bothMindscapesHaveSixDistinctReachableMemoriesAndAFreeLanding() {
		for (RealmKind kind : RealmKind.values()) {
			var sites = RealmLayout.sites(kind);
			assertEquals(6, sites.size());
			Set<String> ids = new HashSet<>();
			for (MemorySite site : sites) {
				assertTrue(ids.add(site.id()));
				assertFalse(site.offeredPath().isBlank());
				assertEquals("realm.powers." + site.id(), site.memoryKey());
				assertEquals("realm.powers.path." + site.offeredPath(), site.pathKey());
				assertTrue(site.rewardEnergy() > 0);
				double dx = site.x() - RealmLayout.ENTRY_X;
				double dz = site.z() - RealmLayout.ENTRY_Z;
				double distanceSquared = dx * dx + dz * dz;
				assertTrue(distanceSquared >= 18 * 18 && distanceSquared <= 64 * 64);
				assertTrue(Math.abs(site.x() - RealmLayout.ENTRY_X) > 2
						|| Math.abs(site.z() - RealmLayout.ENTRY_Z) > 2);
			}
		}
	}
}
