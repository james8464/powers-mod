package com.powers.spell;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class CelestialRuinSavedDataTest {
	@Test
	void codecRoundTripPreservesCountdownAndDestructionCursor() {
		CelestialRuinSavedData.Snapshot snapshot = new CelestialRuinSavedData.Snapshot(
				"powers:dark_realm", 12, 90, -4,
				"123e4567-e89b-12d3-a456-426614174000", 417, true,
				new com.powers.util.BoundedSphereCursor.Snapshot(120, -17, 42, 8, false));
		CelestialRuinSavedData original = new CelestialRuinSavedData(List.of(snapshot));

		JsonElement encoded = CelestialRuinSavedData.CODEC.encodeStart(JsonOps.INSTANCE, original).getOrThrow();
		CelestialRuinSavedData decoded = CelestialRuinSavedData.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow();

		assertEquals(List.of(snapshot), decoded.snapshots());
	}

	@Test
	void codecPreservesWriteAheadDestructiveCommit() {
		var start = new com.powers.util.BoundedSphereCursor.Snapshot(120, -20, 3, 4, false);
		var end = new com.powers.util.BoundedSphereCursor.Snapshot(120, -19, 4, 5, false);
		CelestialRuinSavedData.Snapshot snapshot = new CelestialRuinSavedData.Snapshot(
				"minecraft:overworld", 2, 80, 9,
				"123e4567-e89b-12d3-a456-426614174000", 0, true, start, 42,
				"crater", end, 42);

		JsonElement encoded = CelestialRuinSavedData.CODEC.encodeStart(JsonOps.INSTANCE,
				new CelestialRuinSavedData(List.of(snapshot))).getOrThrow();
		CelestialRuinSavedData decoded = CelestialRuinSavedData.CODEC.parse(
				JsonOps.INSTANCE, encoded).getOrThrow();

		assertEquals(snapshot, decoded.snapshots().getFirst());
		assertFalse(decoded.snapshots().getFirst().pendingPhase().isBlank());
	}
}
