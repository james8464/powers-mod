package com.powers.forge;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.powers.item.artifact.ArtifactAlignment;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrucibleWeaponDataTest {
	@Test
	void levelIsAlwaysRecomputedFromClampedXp() {
		CrucibleWeaponData data = CrucibleWeaponData.create(
				Identifier.fromNamespaceAndPath("powers", "lineage"), ArtifactAlignment.LIGHT, false, 400);
		assertEquals(3, data.level());
		assertEquals(1, data.schemaVersion());
		assertEquals(0, CrucibleWeaponData.create(data.lineageId(), data.alignment(), false, -20).xp());
	}

	@Test
	void codecRoundTripsAndRejectsUnknownSchema() {
		CrucibleWeaponData original = CrucibleWeaponData.create(
				Identifier.fromNamespaceAndPath("powers", "nocturne"), ArtifactAlignment.DARKNESS, true, 225);
		var encoded = CrucibleWeaponData.CODEC.encodeStart(JsonOps.INSTANCE, original).getOrThrow();
		assertEquals(original, CrucibleWeaponData.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow());
		var wrongSchema = JsonParser.parseString("""
				{"schema_version":99,"lineage":"powers:nocturne","alignment":"darkness",
				 "star_bound":false,"xp":0,"level":0}
				""");
		assertFalse(CrucibleWeaponData.CODEC.parse(JsonOps.INSTANCE, wrongSchema).isSuccess());
	}

	@Test
	void starBindingAndXpUpdatesPreserveIdentity() {
		CrucibleWeaponData data = CrucibleWeaponData.create(
				Identifier.fromNamespaceAndPath("powers", "solstice"), ArtifactAlignment.LIGHT, false, 0);
		CrucibleWeaponData bound = data.bindStar().awardXp(100);
		assertTrue(bound.starBound());
		assertEquals(data.lineageId(), bound.lineageId());
		assertEquals(1, bound.level());
	}
}
