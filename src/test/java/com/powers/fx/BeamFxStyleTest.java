package com.powers.fx;

import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BeamFxStyleTest {
	@BeforeAll
	static void bootstrapMinecraftRegistries() {
		net.minecraft.SharedConstants.tryDetectVersion();
		net.minecraft.server.Bootstrap.bootStrap();
	}

	@Test
	void mapsServerParticleOptionsToACompactStableClientStyle() {
		assertEquals(BeamFxStyle.ELECTRIC, BeamFxStyle.from(ParticleTypes.ELECTRIC_SPARK));
		assertEquals(BeamFxStyle.FLAME, BeamFxStyle.from(ParticleTypes.FLAME));
		assertEquals(BeamFxStyle.SOUL, BeamFxStyle.from(ParticleTypes.SOUL_FIRE_FLAME));
		assertEquals(BeamFxStyle.END_ROD, BeamFxStyle.from(ParticleTypes.END_ROD));
		assertEquals(BeamFxStyle.COLORED, BeamFxStyle.from(ColorParticleOption.create(
				ParticleTypes.ENTITY_EFFECT, 0xFF3366CC)));
		assertEquals(BeamFxStyle.COLORED, BeamFxStyle.from(new DustParticleOptions(0x3366CC, 1.25F)));
		assertEquals(0x3366CC, BeamFxStyle.color(new DustParticleOptions(0x3366CC, 1.25F)));
	}

	@Test
	void networkIdsRoundTripAndRejectUnknownValues() {
		for (BeamFxStyle style : BeamFxStyle.values()) {
			assertEquals(style, BeamFxStyle.fromNetworkId(style.networkId()));
		}
		org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
				() -> BeamFxStyle.fromNetworkId(99));
	}
}
