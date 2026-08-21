package com.powers.network;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

final class VesselControlSequenceTest {
	@Test
	void reorderedAndDuplicateInputsCannotReplaceNewerAuthoritativeIntent() {
		UUID owner = UUID.fromString("00000000-0000-0000-0000-000000000919");
		VesselControlSequence sequence = new VesselControlSequence();

		assertTrue(sequence.accept(owner, 4L));
		assertFalse(sequence.accept(owner, 3L));
		assertFalse(sequence.accept(owner, 4L));
		assertTrue(sequence.accept(owner, 5L));
		sequence.clear(owner);
		assertTrue(sequence.accept(owner, 0L));
	}
}
