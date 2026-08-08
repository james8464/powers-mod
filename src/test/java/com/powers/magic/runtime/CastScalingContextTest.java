package com.powers.magic.runtime;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CastScalingContextTest {
	@Test
	void bindingIsNestedAndAlwaysRestored() {
		CastAdjustment outer = new CastAdjustment(true, 1.2, 0.8, 1.1, List.of());
		CastAdjustment inner = new CastAdjustment(true, 0.5, 0.6, 0.7, List.of());

		assertEquals(1.0, CastScalingContext.current().potencyMultiplier());
		CastScalingContext.with(outer, () -> {
			assertEquals(1.2, CastScalingContext.current().potencyMultiplier());
			CastScalingContext.with(inner,
					() -> assertEquals(0.5, CastScalingContext.current().potencyMultiplier()));
			assertEquals(1.2, CastScalingContext.current().potencyMultiplier());
		});
		assertEquals(1.0, CastScalingContext.current().potencyMultiplier());
	}
}
