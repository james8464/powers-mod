package com.powers.entity;

import com.powers.item.artifact.ArtifactAlignment;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestActorProfileTest {
	@Test
	void profileNormalizesEveryPlayerCompatibleControl() {
		var profile = TestActorProfile.of(99, ArtifactAlignment.DARKNESS, false,
				true, false, "  fireball  ");
		assertEquals(10, profile.rank());
		assertEquals(ArtifactAlignment.DARKNESS, profile.alignment());
		assertFalse(profile.consents());
		assertTrue(profile.armored());
		assertFalse(profile.mobile());
		assertEquals("fireball", profile.scriptedAction());
	}
}
