package com.powers.progression;

import com.powers.power.PowerRegistry;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Proves the authored progression table covers every innate at every supported level. */
class InnatePowerLevelsTest {
	@BeforeAll
	static void bootstrapMinecraftRegistries() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
		PowerRegistry.initialize();
	}

	@Test
	void allTwentyThreeInnatesHaveFiniteAuthoredValuesAtAllElevenLevels() {
		assertEquals(23, InnatePowerLevels.powerIds().size());
		for (var power : PowerRegistry.getAll()) {
			String id = power.id().getPath();
			assertTrue(InnatePowerLevels.powerIds().contains(id), id);
			for (int level = 0; level <= 10; level++) {
				InnatePowerLevel profile = InnatePowerLevels.forPower(id, level);
				assertTrue(Double.isFinite(profile.damageMultiplier())
						&& profile.damageMultiplier() >= 1.0, id + " damage " + level);
				assertTrue(Double.isFinite(profile.rangeMultiplier())
						&& profile.rangeMultiplier() >= 1.0, id + " range " + level);
				assertTrue(Double.isFinite(profile.durationMultiplier())
						&& profile.durationMultiplier() >= 1.0, id + " duration " + level);
				assertTrue(profile.destructionTier() >= 0 && profile.destructionTier() <= 10,
						id + " destruction " + level);
				assertTrue(Double.isFinite(profile.capacityMultiplier())
						&& profile.capacityMultiplier() >= 1.0, id + " capacity " + level);
			}
		}
	}

	@Test
	void levelsClampAndPowerShapesRemainMeaningfullyDistinct() {
		assertEquals(InnatePowerLevels.forPower("fireball", 0),
				InnatePowerLevels.forPower("fireball", -100));
		assertEquals(InnatePowerLevels.forPower("fireball", 10),
				InnatePowerLevels.forPower("fireball", 100));
		assertNotEquals(InnatePowerLevels.forPower("fireball", 10),
				InnatePowerLevels.forPower("invisibility", 10));
		assertTrue(InnatePowerLevels.forPower("fireball", 10).destructionTier()
				> InnatePowerLevels.forPower("invisibility", 10).destructionTier());
	}
}
