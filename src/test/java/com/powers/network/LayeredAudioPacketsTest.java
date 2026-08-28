package com.powers.network;

import com.powers.audio.LayeredAudioCue;
import com.powers.testing.network.PacketFaultStreams;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class LayeredAudioPacketsTest {
	private static final Identifier OVERWORLD = Identifier.fromNamespaceAndPath("minecraft", "overworld");

	@Test
	void semanticPayloadSurvivesNetworkRoundTrip() {
		var expected = new LayeredAudioPackets.Payload(19L, LayeredAudioCue.RIFT_OPEN, OVERWORLD,
				12.5, -48.25, 99.75, 0.8F, 1.125F, 640L);
		ByteBuf bytes = Unpooled.buffer();
		try {
			RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(bytes, RegistryAccess.EMPTY);
			LayeredAudioPackets.Payload.STREAM_CODEC.encode(buffer, expected);
			assertEquals(expected, LayeredAudioPackets.Payload.STREAM_CODEC.decode(buffer));
		} finally {
			bytes.release();
		}
	}

	@Test
	void malformedCueIdFailsClosedInsteadOfSelectingFallback() {
		ByteBuf bytes = Unpooled.buffer();
		try {
			RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(bytes, RegistryAccess.EMPTY);
			buffer.writeVarLong(1L);
			buffer.writeByte(255);
			buffer.writeUtf(OVERWORLD.toString(), LayeredAudioPackets.MAX_DIMENSION_UTF8_BYTES);
			buffer.writeDouble(0.0);
			buffer.writeDouble(64.0);
			buffer.writeDouble(0.0);
			buffer.writeFloat(1.0F);
			buffer.writeFloat(1.0F);
			buffer.writeVarLong(20L);
			assertThrows(IllegalArgumentException.class,
					() -> LayeredAudioPackets.Payload.STREAM_CODEC.decode(buffer));
		} finally {
			bytes.release();
		}
	}

	@Test
	void constructorRejectsInvalidIdentifiersCoordinatesAndScalars() {
		assertInvalid(-1L, LayeredAudioCue.RUNE_HUM, OVERWORLD, 0, 64, 0, 1, 1, 1);
		assertInvalid(1L, null, OVERWORLD, 0, 64, 0, 1, 1, 1);
		assertInvalid(1L, LayeredAudioCue.RUNE_HUM, null, 0, 64, 0, 1, 1, 1);
		assertInvalid(1L, LayeredAudioCue.RUNE_HUM, OVERWORLD, Double.NaN, 64, 0, 1, 1, 1);
		assertInvalid(1L, LayeredAudioCue.RUNE_HUM, OVERWORLD, 30_000_001, 64, 0, 1, 1, 1);
		assertInvalid(1L, LayeredAudioCue.RUNE_HUM, OVERWORLD, 0, 64, 0, 0.009F, 1, 1);
		assertInvalid(1L, LayeredAudioCue.RUNE_HUM, OVERWORLD, 0, 64, 0, 4.001F, 1, 1);
		assertInvalid(1L, LayeredAudioCue.RUNE_HUM, OVERWORLD, 0, 64, 0, 1, 0.249F, 1);
		assertInvalid(1L, LayeredAudioCue.RUNE_HUM, OVERWORLD, 0, 64, 0, 1, 4.001F, 1);
		assertInvalid(1L, LayeredAudioCue.RUNE_HUM, OVERWORLD, 0, 64, 0, 1, 1, -1);
	}

	@Test
	void faultStreamKeyIsStablePerEventAndDistinctAcrossEvents() {
		var first = new LayeredAudioPackets.Payload(41L, LayeredAudioCue.BEAM_RING, OVERWORLD,
				0, 64, 0, 1, 1, 100);
		var sameEvent = new LayeredAudioPackets.Payload(41L, LayeredAudioCue.DARK_WHISPER, OVERWORLD,
				100, 70, 100, 0.5F, 0.8F, 100);
		var next = new LayeredAudioPackets.Payload(42L, LayeredAudioCue.BEAM_RING, OVERWORLD,
				0, 64, 0, 1, 1, 100);
		assertEquals(PacketFaultStreams.key(first), PacketFaultStreams.key(sameEvent));
		assertNotEquals(PacketFaultStreams.key(first), PacketFaultStreams.key(next));
	}

	private static void assertInvalid(long eventId, LayeredAudioCue cue, Identifier dimension,
			double x, double y, double z, float gain, float pitch, long emittedGameTime) {
		assertThrows(RuntimeException.class, () -> new LayeredAudioPackets.Payload(eventId, cue,
				dimension, x, y, z, gain, pitch, emittedGameTime));
	}
}
