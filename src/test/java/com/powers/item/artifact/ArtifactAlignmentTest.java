package com.powers.item.artifact;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ArtifactAlignmentTest {
	@Test
	void serializedNamesAreStableAndStrict() {
		assertEquals(ArtifactAlignment.DARKNESS, ArtifactAlignment.fromSerialized("darkness"));
		assertEquals(ArtifactAlignment.LIGHT, ArtifactAlignment.fromSerialized("light"));
		assertThrows(IllegalArgumentException.class, () -> ArtifactAlignment.fromSerialized("neutral"));
	}
}
