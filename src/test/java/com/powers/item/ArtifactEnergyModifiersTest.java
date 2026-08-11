package com.powers.item;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ArtifactEnergyModifiersTest {
	@Test
	void malignemberOnlyDiscountsExplicitlyDestructiveMagic() {
		assertEquals(16, ArtifactEnergyModifiers.activationCost(true, "fireball", 20));
		assertEquals(80, ArtifactEnergyModifiers.activationCost(true, "celestial_ruin", 100));
		assertEquals(20, ArtifactEnergyModifiers.activationCost(true, "forcefield", 20));
		assertEquals(20, ArtifactEnergyModifiers.activationCost(false, "fireball", 20));
	}

	@Test
	void discountsNeverMakeAnAuthoredCostFree() {
		assertEquals(1, ArtifactEnergyModifiers.activationCost(true, "void_beam", 1));
		assertEquals(0, ArtifactEnergyModifiers.activationCost(true, "void_beam", 0));
	}
}
