package com.powers.item;

import com.powers.ImportedItemRules;
import com.powers.ImportedPackItems;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

class ImportedItemPurposeTest {
	@Test
	void everyVisibleImportedItemHasAPurposeAndSurvivalAcquisitionFamily() {
		for (String texture : ImportedPackItems.textureIds()) {
			if (ImportedItemRules.isLegacyAssetLayer(texture)) continue;
			var purpose = ImportedItemPurpose.describe(texture);
			assertFalse(purpose.role().isBlank(), texture);
			assertFalse(purpose.acquisition().isBlank(), texture);
		}
	}
}
