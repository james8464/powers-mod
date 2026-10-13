package com.powers.thermal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ThermalTransitionTableTest {
	@Test
	void everyInfluenceHasOneExplicitTransitionForEveryMaterial() {
		for (ThermalInfluence influence : ThermalInfluence.values()) {
			for (ThermalMaterial material : ThermalMaterial.values()) {
				ThermalTransition transition = ThermalTransitionTable.resolve(influence, material);
				assertEquals(material, transition.source());
			}
		}
		assertEquals(ThermalInfluence.values().length * ThermalMaterial.values().length,
				ThermalTransitionTable.entryCount());
	}

	@Test
	void iceFireWaterSnowAndPlantsHaveNonContradictoryOutcomes() {
		assertEquals(ThermalOutcome.FREEZE,
				ThermalTransitionTable.resolve(ThermalInfluence.ICE, ThermalMaterial.WATER).outcome());
		assertEquals(ThermalOutcome.SOLIDIFY,
				ThermalTransitionTable.resolve(ThermalInfluence.ICE, ThermalMaterial.LAVA).outcome());
		assertEquals(ThermalOutcome.EXTINGUISH,
				ThermalTransitionTable.resolve(ThermalInfluence.ICE, ThermalMaterial.FIRE).outcome());
		assertEquals(ThermalOutcome.COAT,
				ThermalTransitionTable.resolve(ThermalInfluence.ICE, ThermalMaterial.AIR).outcome());
		assertEquals(ThermalOutcome.MELT,
				ThermalTransitionTable.resolve(ThermalInfluence.FIREBALL, ThermalMaterial.ICE).outcome());
		assertEquals(ThermalOutcome.EVAPORATE,
				ThermalTransitionTable.resolve(ThermalInfluence.INFERNO, ThermalMaterial.SNOW).outcome());
		assertEquals(ThermalOutcome.SCORCH,
				ThermalTransitionTable.resolve(ThermalInfluence.FIREBALL, ThermalMaterial.PLANT).outcome());
		assertEquals(ThermalOutcome.GROW,
				ThermalTransitionTable.resolve(ThermalInfluence.GROWTH, ThermalMaterial.PLANT).outcome());
	}

	@Test
	void realmBlocksAreStableUnderEveryInfluence() {
		for (ThermalInfluence influence : ThermalInfluence.values()) {
			assertEquals(ThermalOutcome.KEEP,
					ThermalTransitionTable.resolve(influence, ThermalMaterial.PURE_LIGHT).outcome());
			assertEquals(ThermalOutcome.KEEP,
					ThermalTransitionTable.resolve(influence, ThermalMaterial.DARKNESS).outcome());
		}
	}
}
