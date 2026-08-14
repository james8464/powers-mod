package com.powers.network;

import com.powers.fx.FxLodTier;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Guards the bounded distant-event sound vocabulary and its compact wire contract. */
class EventAudioPacketsTest {
	@Test
	void boundedCueSurvivesNetworkRoundTrip() {
		var expected = new EventAudioPackets.Payload(
				EventAudioPackets.Cue.LIGHT_HERALD, FxLodTier.FAR, 0.65F);
		ByteBuf bytes = Unpooled.buffer();
		try {
			RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(bytes, RegistryAccess.EMPTY);
			EventAudioPackets.Payload.STREAM_CODEC.encode(buffer, expected);
			assertEquals(expected, EventAudioPackets.Payload.STREAM_CODEC.decode(buffer));
		} finally {
			bytes.release();
		}
	}

	@Test
	void distantLayerIsRestrainedAndHiddenCuesAreRejected() {
		assertEquals(0.55F, EventAudioPackets.Cue.DARK_EVENT.volume(FxLodTier.MID));
		assertEquals(0.28F, EventAudioPackets.Cue.DARK_EVENT.volume(FxLodTier.FAR));
		assertThrows(IllegalArgumentException.class, () -> new EventAudioPackets.Payload(
				EventAudioPackets.Cue.DARK_EVENT, FxLodTier.HIDDEN, 1.0F));
	}
}
