package com.powers.item;

import com.powers.ImportedPackItems;
import com.powers.PowersWeapons;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

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

	@Test
	void catalogueUsesOnlyImplementedEnergyArtifactTerminology() throws Exception {
		String document = Files.readString(Path.of("docs/gameplay/item-catalogue.md"));
		assertTrue(document.contains("Energy reservoir"));
		assertTrue(document.contains("Overrides every player-consent gate"));
		assertTrue(document.contains("five-minute lethal-damage ward"));
		assertTrue(document.contains("Arcane energy dust"));
		assertFalse(document.contains("Soul vessel"));
		assertFalse(document.contains("amplify the next grimoire ritual"));
		assertFalse(document.contains("Essence Dust"));
	}
}
