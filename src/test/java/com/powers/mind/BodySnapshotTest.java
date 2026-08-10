package com.powers.mind;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BodySnapshotTest {
	@Test
	void codecRoundTripsEveryFrozenAppearanceField() {
		BodySnapshot snapshot = sample();
		JsonElement encoded = BodySnapshot.CODEC.encodeStart(JsonOps.INSTANCE, snapshot).getOrThrow();
		BodySnapshot decoded = BodySnapshot.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow();

		assertEquals(snapshot, decoded);
		assertEquals(0x7F, decoded.profile().modelParts());
		assertEquals("left", decoded.pose().mainArm());
		assertEquals("off_hand", decoded.animation().usedHand());
		assertEquals(1.25F, decoded.pose().scale());
	}

	@Test
	void rejectsUnboundedOrNonFiniteSnapshotData() {
		BodySnapshot sample = sample();
		assertThrows(IllegalArgumentException.class, () -> new BodySnapshot.Profile(
				sample.profile().id(), "x".repeat(65), 0, List.of()));
		assertThrows(IllegalArgumentException.class, () -> new BodySnapshot.Profile(
				sample.profile().id(), "owner", 0, List.of("a", "b", "c", "d", "e", "f", "g")));
		assertThrows(IllegalArgumentException.class, () -> new BodySnapshot.PoseState(
				"standing", "right", "", Float.NaN, 0, 0, 0, 1, 0, 0, 0));
		assertThrows(IllegalArgumentException.class, () -> new BodySnapshot.PoseState(
				"standing", "right", "", 0, 0, 0, 0, 20, 0, 0, 0));
	}

	private static BodySnapshot sample() {
		return new BodySnapshot(
				new BodySnapshot.Profile(UUID.fromString("00000000-0000-0000-0000-000000000123"),
						"owner", 0x7F, List.of("minecraft:diamond_helmet#1#3", "minecraft:air#0#0")),
				new BodySnapshot.PoseState("crouching", "left", "north",
						45.0F, -15.0F, 30.0F, 40.0F, 1.25F, 0.1, 0.0, -0.2),
				new BodySnapshot.AnimationState(true, "main_hand", 3, 0.65F,
						2.5F, 0.8F, true, "off_hand", 12));
	}
}
