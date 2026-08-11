package com.powers.item;

import com.powers.ImportedPackItems;
import com.powers.PowersWeapons;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemCatalogueDocumentationTest {
	@Test
	void generatedCatalogueCoversEveryRegisteredImportedAndWeaponItem() throws Exception {
		String document = Files.readString(Path.of("docs/gameplay/item-catalogue.md"));
		for (String item : ImportedPackItems.importedIds()) {
			assertTrue(document.contains("`powers:" + item + "`"), item);
		}
		for (String item : PowersWeapons.allWeaponIds()) {
			assertTrue(document.contains("`powers:" + item + "`"), item);
		}
		for (String core : java.util.List.of("rainbow_crystal", "dark_crystal", "light_crystal",
				"darkness", "pure_light", "amethyst_ward", "arcane_crucible",
				"power_test_actor_spawn_egg", "first_vessel_spawn_egg")) {
			assertTrue(document.contains("`powers:" + core + "`"), core);
		}
	}
}
