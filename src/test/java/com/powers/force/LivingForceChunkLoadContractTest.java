package com.powers.force;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LivingForceChunkLoadContractTest {
	@Test
	void chunkLoadRepairReadsTheDeliveredChunkWithoutReenteringTheLevelLoader() throws Exception {
		String source = Files.readString(Path.of(
				"src/main/java/com/powers/force/LivingForceManager.java"));
		int start = source.indexOf("private static void loadFrontier");
		int end = source.indexOf("private static void refreshFrontier", start);
		String method = source.substring(start, end);

		assertTrue(method.contains("chunk.getBlockState(position)"));
		assertFalse(method.contains("level.getBlockState(position)"));
	}
}
