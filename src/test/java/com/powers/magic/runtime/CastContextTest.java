package com.powers.magic.runtime;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CastContextTest {
	@Test
	void eachCastSourceOwnsExactlyOneScalingPolicy() {
		assertEquals(ScalingPolicy.INNATE_RANK, CastContext.forSource(CastSource.INNATE).scalingPolicy());
		assertEquals(ScalingPolicy.ARTIFACT, CastContext.forSource(CastSource.ARTIFACT).scalingPolicy());
		assertEquals(ScalingPolicy.UNRANKED, CastContext.forSource(CastSource.CRYSTAL).scalingPolicy());
		assertEquals(ScalingPolicy.UNRANKED, CastContext.forSource(CastSource.SPELL).scalingPolicy());
	}
}
