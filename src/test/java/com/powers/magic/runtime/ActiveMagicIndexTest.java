package com.powers.magic.runtime;

import com.powers.magic.MagicActionId;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActiveMagicIndexTest {
	private static final UUID OWNER = UUID.fromString("11111111-1111-1111-1111-111111111111");

	@Test
	void nearbyReturnsOnlyIntersectingLivePresences() {
		ActiveMagicIndex index = new ActiveMagicIndex(16);
		index.register(presence("00000000-0000-0000-0000-000000000001", "near", "overworld",
				4, 64, 4, 8, 100));
		index.register(presence("00000000-0000-0000-0000-000000000002", "far", "overworld",
				96, 64, 96, 8, 100));
		index.register(presence("00000000-0000-0000-0000-000000000003", "other_dimension", "nether",
				4, 64, 4, 8, 100));

		assertEquals(Set.of("near"), actions(index.nearby("overworld", 0, 64, 0, 16, 50)));
		assertTrue(index.nearby("overworld", 0, 64, 0, 16, 101).isEmpty());
	}

	@Test
	void movingPresenceUpdatesCellsWithoutLeavingGhostMembership() {
		ActiveMagicIndex index = new ActiveMagicIndex(16);
		MagicPresence presence = presence("00000000-0000-0000-0000-000000000004", "moving",
				"overworld", 0, 64, 0, 2, 200);
		index.register(presence);

		index.move(presence.id(), "overworld", PresenceAnchor.fixed(80, 64, 80));

		assertTrue(index.nearby("overworld", 0, 64, 0, 4, 20).isEmpty());
		assertEquals(Set.of("moving"), actions(index.nearby("overworld", 80, 64, 80, 4, 20)));
	}

	@Test
	void rebindMovesAndExtendsPresenceToPhysicalLifetime() {
		ActiveMagicIndex index = new ActiveMagicIndex(16);
		MagicPresence presence = presence("00000000-0000-0000-0000-000000000008", "projectile",
				"overworld", 0, 64, 0, 2, 20);
		index.register(presence);

		assertTrue(index.rebind(presence.id(), "nether",
				PresenceAnchor.entity(PresenceAnchor.Kind.PROJECTILE, UUID.randomUUID(), 40, 70, 40), 80));

		assertTrue(index.nearby("overworld", 0, 64, 0, 4, 10).isEmpty());
		assertEquals(Set.of("projectile"), actions(index.nearby("nether", 40, 70, 40, 4, 50)));
		assertTrue(index.nearby("nether", 40, 70, 40, 4, 81).isEmpty());
	}

	@Test
	void ownerAndGlobalCleanupRemovePresenceAndCellState() {
		ActiveMagicIndex index = new ActiveMagicIndex(16);
		index.register(presence("00000000-0000-0000-0000-000000000005", "one", "overworld",
				0, 64, 0, 20, 100));
		index.register(new MagicPresence(
				new MagicPresenceId(UUID.fromString("00000000-0000-0000-0000-000000000006")),
				new MagicActionId("two"), UUID.fromString("22222222-2222-2222-2222-222222222222"),
				"overworld", PresenceAnchor.fixed(48, 64, 48), 4, 100));

		index.removeOwner(OWNER);
		assertEquals(1, index.size());
		index.clear();
		assertEquals(0, index.size());
		assertEquals(0, index.cellCount());
	}

	@Test
	void registrationRejectsDuplicateIdsAndUnsafeGeometry() {
		ActiveMagicIndex index = new ActiveMagicIndex(16);
		MagicPresence presence = presence("00000000-0000-0000-0000-000000000007", "safe",
				"overworld", 0, 64, 0, 2, 100);
		index.register(presence);

		assertThrows(IllegalArgumentException.class, () -> index.register(presence));
		assertThrows(IllegalArgumentException.class, () -> PresenceAnchor.fixed(Double.NaN, 64, 0));
		assertThrows(IllegalArgumentException.class, () -> new ActiveMagicIndex(0));
	}

	private static MagicPresence presence(String id, String action, String dimension,
			double x, double y, double z, double radius, long expiresAt) {
		return new MagicPresence(new MagicPresenceId(UUID.fromString(id)), new MagicActionId(action),
				OWNER, dimension, PresenceAnchor.fixed(x, y, z), radius, expiresAt);
	}

	private static Set<String> actions(Iterable<MagicPresence> presences) {
		return java.util.stream.StreamSupport.stream(presences.spliterator(), false)
				.map(presence -> presence.action().value()).collect(Collectors.toUnmodifiableSet());
	}
}
