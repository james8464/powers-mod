package com.powers.item.artifact;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ArtifactMenuRulesTest {
	@Test
	void groupsAndPagesRemainSmallAndDeterministic() {
		List<ArtifactActionDefinition> all = ArtifactActionCatalogue.forAlignment(
				ArtifactAlignment.DARKNESS);
		for (ArtifactActionCategory category : ArtifactActionCategory.values()) {
			var group = ArtifactMenuRules.group(all, category);
			for (int page = 0; page < ArtifactMenuRules.pageCount(group); page++) {
				assertFalse(ArtifactMenuRules.page(group, page).size()
						> ArtifactMenuRules.PAGE_SIZE);
			}
		}
	}

	@Test
	void malformedParallelPacketListsFallBackSafely() {
		assertEquals(4, ArtifactMenuRules.valueAt(List.of(4), 0, 9));
		assertEquals(9, ArtifactMenuRules.valueAt(List.of(4), 3, 9));
		assertEquals(false, ArtifactMenuRules.valueAt(List.of(true), 4, false));
	}

	@Test
	void selectionVariantsUseAbilityPathsRatherThanIdentifierStringEquality() {
		assertEquals(-1, ArtifactMenuRules.selectionVariant("elemental_blast", 6, 2));
		assertEquals(6, ArtifactMenuRules.selectionVariant("size_shift", 6, 2));
		assertEquals(2, ArtifactMenuRules.selectionVariant("gravity_displacement", 6, 2));
		assertEquals(-1, ArtifactMenuRules.selectionVariant("flight", 6, 2));
	}

	@Test
	void malformedGravityVariantsFallBackToNeutralOrbit() {
		assertEquals(1, ArtifactMenuRules.normalizeGravityOption(-1));
		assertEquals(1, ArtifactMenuRules.normalizeGravityOption(99));
		assertEquals(0, ArtifactMenuRules.normalizeGravityOption(0));
		assertEquals(2, ArtifactMenuRules.normalizeGravityOption(2));
	}
}
