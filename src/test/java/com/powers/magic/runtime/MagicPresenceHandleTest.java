package com.powers.magic.runtime;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MagicPresenceHandleTest {
	@Test
	void entityKindsRequireABoundEntityWhileFixedKindsRejectOne() {
		MagicPresenceId id = new MagicPresenceId(UUID.randomUUID());
		UUID entity = UUID.randomUUID();
		MagicPresenceHandle projectile = MagicPresenceHandle.entity(
				id, MagicPresenceHandle.Kind.PROJECTILE, entity);
		assertEquals(entity, projectile.boundEntity());

		assertThrows(IllegalArgumentException.class, () ->
				MagicPresenceHandle.entity(id, MagicPresenceHandle.Kind.IMPACT, entity));
		assertEquals(MagicPresenceHandle.Kind.FIELD,
				MagicPresenceHandle.fixed(id, MagicPresenceHandle.Kind.FIELD).kind());
	}
}
