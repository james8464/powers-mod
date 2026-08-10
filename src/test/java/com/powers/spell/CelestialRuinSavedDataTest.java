package com.powers.spell;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
