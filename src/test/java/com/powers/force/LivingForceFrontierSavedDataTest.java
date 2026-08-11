package com.powers.force;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LivingForceFrontierSavedDataTest {
	@Test
	void compactSnapshotRoundTripsBothForceKindsDeterministically() {
		Map<Long, LivingForceKind> frontier = new LinkedHashMap<>();
		frontier.put(8L, LivingForceKind.PURE_LIGHT);
		frontier.put(-4L, LivingForceKind.DARKNESS);
		frontier.put(2L, LivingForceKind.DARKNESS);

		String encoded = LivingForceFrontierSavedData.encodeEntry("powers:dark_realm", 7L, frontier);
		var decoded = LivingForceFrontierSavedData.decodeEntry(encoded).orElseThrow();
		assertEquals("powers:dark_realm", decoded.dimension());
		assertEquals(7L, decoded.chunk());
		assertEquals(frontier, decoded.positions());
		assertEquals(encoded, LivingForceFrontierSavedData.encodeEntry(
				decoded.dimension(), decoded.chunk(), decoded.positions()));
	}

	@Test
	void corruptRowsFailClosedWithoutDiscardingValidRows() {
		LivingForceFrontierSavedData data = new LivingForceFrontierSavedData(java.util.List.of(
				"broken", LivingForceFrontierSavedData.encodeEntry("minecraft:overworld", 2L,
						Map.of(4L, LivingForceKind.DARKNESS))));

		assertTrue(data.hasChunk("minecraft:overworld", 2L));
		assertEquals(Map.of(4L, LivingForceKind.DARKNESS),
				data.frontier("minecraft:overworld", 2L));
		assertFalse(data.hasChunk("minecraft:overworld", 3L));
	}

	@Test
	void updatesKeepOneCanonicalKindPerPosition() {
		LivingForceFrontierSavedData data = new LivingForceFrontierSavedData();
		data.replaceChunk("minecraft:overworld", 1L, Map.of());
		data.update("minecraft:overworld", 1L, 9L, LivingForceKind.DARKNESS);
		data.update("minecraft:overworld", 1L, 9L, LivingForceKind.PURE_LIGHT);
		assertEquals(Map.of(9L, LivingForceKind.PURE_LIGHT),
				data.frontier("minecraft:overworld", 1L));
		data.update("minecraft:overworld", 1L, 9L, null);
		assertTrue(data.frontier("minecraft:overworld", 1L).isEmpty());
	}
}
