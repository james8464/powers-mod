package com.powers.item.artifact;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ArtifactRevocationCoverageTest {
	@Test
	void oneRevokerOwnsEveryLongLivedArtifactState() throws Exception {
		String source = Files.readString(Path.of(System.getProperty("user.dir"),
				"src/main/java/com/powers/item/ArtifactOwnedStateRevoker.java"));
		for (String owner : new String[] {
				"ArtifactFieldManager", "ArtifactGateManager", "ArtifactGroundWorkQueue",
				"ArtifactGuardianSummons", "ArtifactDeathWardManager", "ArtifactCovenantManager",
				"ArtifactChainManager"
		}) {
			assertTrue(source.contains(owner), () -> "Missing revocation owner: " + owner);
		}
	}
}
