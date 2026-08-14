package com.powers.network;

import com.powers.fx.FxLodTier;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Guards the observer-specific Celestial Ruin tier carried to the client renderer. */
class CelestialRuinPacketsTest {
	@Test
	void observerTierSurvivesNetworkRoundTrip() {
		var expected = new CelestialRuinPackets.Payload(
				CelestialRuinPackets.Phase.SUSTAIN, 10.5, -20.0, 30.25, 87, FxLodTier.FAR);
		ByteBuf bytes = Unpooled.buffer();
		try {
			RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(bytes, RegistryAccess.EMPTY);
			CelestialRuinPackets.Payload.STREAM_CODEC.encode(buffer, expected);
			assertEquals(expected, CelestialRuinPackets.Payload.STREAM_CODEC.decode(buffer));
		} finally {
			bytes.release();
		}
	}

	@Test
	void nonFiniteAndHiddenCuesFailClosed() {
		assertThrows(IllegalArgumentException.class, () -> new CelestialRuinPackets.Payload(
				CelestialRuinPackets.Phase.BEGIN, Double.NaN, 0.0, 0.0, 0, FxLodTier.NEAR));
		assertThrows(IllegalArgumentException.class, () -> new CelestialRuinPackets.Payload(
				CelestialRuinPackets.Phase.END, 0.0, 0.0, 0.0, 0, FxLodTier.HIDDEN));
	}
}
