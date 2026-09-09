package com.powers.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CrystalSelectorPacketsTest {
	@Test
	void discoveredModesSurviveTheRealSelectorPayloadCodec() {
		var expected = new CrystalSelectorPackets.OpenPayload(42L,
				List.of("inferno", "life_bloom", "middleworld"),
				List.of("life_bloom"), 2);
		ByteBuf bytes = Unpooled.buffer();
		try {
			RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(bytes, RegistryAccess.EMPTY);
			CrystalSelectorPackets.OpenPayload.STREAM_CODEC.encode(buffer, expected);
			assertEquals(expected, CrystalSelectorPackets.OpenPayload.STREAM_CODEC.decode(buffer));
		} finally {
			bytes.release();
		}
	}
}
