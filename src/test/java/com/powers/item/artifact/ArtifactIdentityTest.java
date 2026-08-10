package com.powers.item.artifact;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

class ArtifactIdentityTest {
	@Test
	void identityCodecPreservesStableItemAndAlignment() {
		ArtifactIdentity identity = new ArtifactIdentity("shadow_sword", ArtifactAlignment.DARKNESS);
		var encoded = ArtifactIdentity.CODEC.encodeStart(JsonOps.INSTANCE, identity).getOrThrow();
		assertEquals(identity, ArtifactIdentity.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow());
	}
}
