package com.powers.magic.runtime;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PhysicalCollisionFamilyTest {
	@Test
	void allSixPhysicalFamiliesAreClassifiedSymmetrically() {
		assertSymmetric(MagicPresenceHandle.Kind.BEAM, MagicPresenceHandle.Kind.BEAM,
				PhysicalCollisionFamily.BEAM_BEAM);
		assertSymmetric(MagicPresenceHandle.Kind.PROJECTILE, MagicPresenceHandle.Kind.PROJECTILE,
				PhysicalCollisionFamily.PROJECTILE_PROJECTILE);
		assertSymmetric(MagicPresenceHandle.Kind.PROJECTILE, MagicPresenceHandle.Kind.FIELD,
				PhysicalCollisionFamily.PROJECTILE_FIELD);
		assertSymmetric(MagicPresenceHandle.Kind.BEAM, MagicPresenceHandle.Kind.FIELD,
				PhysicalCollisionFamily.BEAM_FIELD);
		assertSymmetric(MagicPresenceHandle.Kind.FORCE_BLOCK, MagicPresenceHandle.Kind.IMPACT,
				PhysicalCollisionFamily.FORCE_BLOCK);
		assertSymmetric(MagicPresenceHandle.Kind.ENTITY, MagicPresenceHandle.Kind.FIELD,
				PhysicalCollisionFamily.BODY_FIELD);
	}

	private static void assertSymmetric(MagicPresenceHandle.Kind first,
			MagicPresenceHandle.Kind second, PhysicalCollisionFamily expected) {
		assertEquals(expected, PhysicalCollisionFamily.of(first, second));
		assertEquals(expected, PhysicalCollisionFamily.of(second, first));
	}
}
