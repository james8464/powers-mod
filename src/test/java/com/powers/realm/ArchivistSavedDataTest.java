package com.powers.realm;

import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArchivistSavedDataTest {
	private static final UUID FIRST = UUID.fromString("00000000-0000-0000-0000-000000000001");
	private static final UUID OTHER = UUID.fromString("00000000-0000-0000-0000-000000000002");

	@Test
	void persistedOwnerCannotBeStolenOrReleasedByAnotherEntity() {
		var original = new ArchivistSavedData();
		original.claim("archive", FIRST);
		var encoded = ArchivistSavedData.CODEC.encodeStart(JsonOps.INSTANCE, original).getOrThrow();
		var restored = ArchivistSavedData.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow();
		assertTrue(restored.occupied("archive"));
		restored.claim("archive", OTHER);
		restored.release("archive", OTHER);
		assertTrue(restored.occupied("archive"));
		restored.release("archive", FIRST);
		assertFalse(restored.occupied("archive"));
	}

	@Test
	void destroyingOneOwnerDoesNotReleaseAnotherOutpost() {
		var data = new ArchivistSavedData();
		data.claim("archive", FIRST);
		data.claim("observatory", OTHER);
		data.release("archive", FIRST);
		assertFalse(data.occupied("archive"));
		assertTrue(data.occupied("observatory"));
	}
}
