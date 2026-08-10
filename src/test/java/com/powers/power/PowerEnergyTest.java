package com.powers.power;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PowerEnergyTest {
	@Test
	void capacityArithmeticCannotOverflowOnCorruptInputs() {
		assertEquals(Integer.MAX_VALUE, PowerEnergy.maxCapacity(Integer.MAX_VALUE));
		assertEquals(Integer.MAX_VALUE, PowerEnergy.darknessMaxCapacity(Integer.MAX_VALUE));
		assertEquals(PowerEnergy.BASE_MAX, PowerEnergy.maxCapacity(Integer.MIN_VALUE));
		assertEquals(PowerEnergy.DARKNESS_BASE_MAX,
				PowerEnergy.darknessMaxCapacity(Integer.MIN_VALUE));
	}
}
