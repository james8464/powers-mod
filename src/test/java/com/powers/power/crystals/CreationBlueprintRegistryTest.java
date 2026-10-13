package com.powers.power.crystals;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreationBlueprintRegistryTest {
	@Test
	void datapackBlockBlueprintEnforcesBudgetsAndMaterialAllowlist() {
		var valid = CreationBlueprintParser.parse("powers:test", object("""
				{"name":"Test shelter","blocks":[
				 {"pos":[0,0,0],"block":"minecraft:orange_concrete"},
				 {"pos":[1,0,0],"block":"minecraft:glowstone"}]}
				"""));
		assertEquals(2, valid.blocks().size());
		assertEquals("Test shelter", valid.name());

		assertThrows(IllegalArgumentException.class, () -> CreationBlueprintParser.parse(
				"powers:unsafe", object("""
						{"name":"Unsafe","blocks":[{"pos":[0,0,0],"block":"minecraft:chest"}]}
						""")));
		assertThrows(IllegalArgumentException.class, () -> CreationBlueprintParser.parse(
				"powers:oversize", object("""
						{"name":"Wide","blocks":[{"pos":[16,0,0],"block":"minecraft:stone"}]}
						""")));
	}

	@Test
	void vanillaTemplatesAreAnExplicitSmallAllowlist() {
		var approved = CreationBlueprintParser.parse("powers:plains", object("""
				{"name":"Plains shelter","template":"minecraft:village/plains/houses/plains_small_house_1"}
				"""));
		assertTrue(approved.isVanillaTemplate());
		assertThrows(IllegalArgumentException.class, () -> CreationBlueprintParser.parse(
				"powers:monument", object("""
						{"name":"Nope","template":"minecraft:monument/monument"}
						""")));
	}

	@Test
	void publicationIsAtomicAndSelectionOrderIsStable() {
		CreationBlueprintRegistry registry = new CreationBlueprintRegistry();
		assertTrue(registry.reload(List.of(
				document("powers:z", "{\"name\":\"Zed\",\"blocks\":[{\"pos\":[0,0,0],\"block\":\"minecraft:stone\"}]}"),
				document("powers:a", "{\"name\":\"Alpha\",\"blocks\":[{\"pos\":[0,0,0],\"block\":\"minecraft:glass\"}]}"))));
		assertEquals(List.of("powers:a", "powers:z"), registry.ids());

		assertFalse(registry.reload(List.of(
				document("powers:safe", "{\"name\":\"Safe\",\"blocks\":[{\"pos\":[0,0,0],\"block\":\"minecraft:stone\"}]}"),
				document("powers:unsafe", "{\"name\":\"Unsafe\",\"blocks\":[{\"pos\":[0,0,0],\"block\":\"minecraft:spawner\"}]}"))));
		assertEquals(List.of("powers:a", "powers:z"), registry.ids());
		assertEquals("powers:z", CreationBlueprintSelection.next("powers:a", registry.ids()));
		assertEquals("powers:a", CreationBlueprintSelection.next("missing", registry.ids()));
	}

	@Test
	void lookUpGestureDoesNotTrapTheOuterOrangeCrystalModeCycle() {
		assertFalse(CreationBlueprintSelection.isSelectionGesture(true, 0.0F));
		assertFalse(CreationBlueprintSelection.isSelectionGesture(false, -90.0F));
		assertTrue(CreationBlueprintSelection.isSelectionGesture(true, -71.0F));
	}

	private static CreationBlueprintRegistry.Document document(String id, String json) {
		return new CreationBlueprintRegistry.Document(id, object(json));
	}

	private static com.google.gson.JsonObject object(String json) {
		return JsonParser.parseString(json).getAsJsonObject();
	}
}
