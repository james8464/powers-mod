package com.powers.item;

import com.powers.ImportedItemRules;
import com.powers.ImportedPackItems;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class ArtifactRoleCatalogueTest {
	@Test
	void everyVisibleNonFoodImportHasAnExplicitPurpose() {
		for (String texture : ImportedPackItems.textureIds()) {
			if (texture.startsWith("food_") || ImportedItemRules.isHiddenCompatibilityItem(texture)) continue;
			assertNotEquals(ArtifactRole.NONE, ArtifactRoleCatalogue.role(texture), texture);
		}
	}

	@Test
	void importantFamiliesKeepDistinctEnergyOnlyRoles() {
		assertEquals(ArtifactRole.ENERGY_RESERVOIR,
				ArtifactRoleCatalogue.role("artifact_soulmatrix"));
		assertEquals(ArtifactRole.CONSENT_OVERRIDE,
				ArtifactRoleCatalogue.role("artifact_emperyeanjewel"));
		assertEquals(ArtifactRole.ARCANE_ENERGY_DUST,
				ArtifactRoleCatalogue.role("magic_essence_soul_dust"));
		assertEquals(ArtifactRole.HEALTH_TO_ENERGY,
				ArtifactRoleCatalogue.role("artifact_ritualdagger"));
		assertEquals(ArtifactRole.GRIMOIRE,
				ArtifactRoleCatalogue.role("book_grimoire_wild"));
	}
}
