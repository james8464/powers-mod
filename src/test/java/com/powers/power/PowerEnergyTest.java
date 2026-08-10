package com.powers.power;

import org.junit.jupiter.api.Test;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PowerEnergyTest {
	@BeforeAll
	static void bootstrapMinecraftRegistries() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	@Test
	void capacityArithmeticCannotOverflowOnCorruptInputs() {
		assertEquals(Integer.MAX_VALUE, PowerEnergy.maxCapacity(Integer.MAX_VALUE));
		assertEquals(Integer.MAX_VALUE, PowerEnergy.darknessMaxCapacity(Integer.MAX_VALUE));
		assertEquals(PowerEnergy.BASE_MAX, PowerEnergy.maxCapacity(Integer.MIN_VALUE));
		assertEquals(PowerEnergy.DARKNESS_BASE_MAX,
				PowerEnergy.darknessMaxCapacity(Integer.MIN_VALUE));
	}

	@Test
	void everyIndefiniteInnateToggleHasPositiveUpkeepAndTimeFreezeCostsMost() {
		PowerRegistry.initialize();
		int timeFreeze = PowerEnergy.ongoingCost(PowerRegistry.get("time_freeze").ability());
		for (Power power : PowerRegistry.getAll()) {
			if (!power.ability().isToggle()) continue;
			int upkeep = PowerEnergy.ongoingCost(power.ability());
			assertTrue(upkeep > 0, power.id().toString());
			if (!power.id().getPath().equals("time_freeze")) {
				assertTrue(timeFreeze > upkeep, power.id().toString());
			}
		}
	}
}
