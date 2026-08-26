package com.powers.network;

import com.powers.fx.ClientRankTenSilhouetteState;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Guards every field and validation boundary of the compact rank-ten silhouette payload. */
class RankTenSilhouettePacketsTest {
	private static final UUID CASTER = UUID.fromString("f17ba84f-701d-4fa9-8ba5-886039fde8a1");

	@Test
	void everyFieldSurvivesNetworkRoundTrip() {
		var expected = new RankTenSilhouettePackets.Payload(91L, 22, CASTER,
				"minecraft:overworld", 12.25, -31.5, 4096.75, 179.5F, -89.25F,
				1, 0x5EEDC0DE, ClientRankTenSilhouetteState.AUTHORED_LIFETIME_TICKS);
		ByteBuf bytes = Unpooled.buffer();
		try {
			RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(bytes, RegistryAccess.EMPTY);
			RankTenSilhouettePackets.Payload.STREAM_CODEC.encode(buffer, expected);
			var actual = RankTenSilhouettePackets.Payload.STREAM_CODEC.decode(buffer);
			assertEquals(expected, actual);
			assertEquals(expected.wire(), actual.wire());
		} finally {
			bytes.release();
		}
	}

	@Test
	void constructorRejectsUnknownIdentitySessionAndGeometry() {
		assertInvalid(payload(0L, 0, CASTER, "minecraft:overworld", 0, 0, 0, 0, 0, 0, 1));
		assertInvalid(payload(1L, 23, CASTER, "minecraft:overworld", 0, 0, 0, 0, 0, 0, 1));
		assertInvalid(payload(1L, 0, null, "minecraft:overworld", 0, 0, 0, 0, 0, 0, 1));
		assertInvalid(payload(1L, 0, new UUID(0, 0), "minecraft:overworld", 0, 0, 0, 0, 0, 0, 1));
		assertInvalid(payload(1L, 0, CASTER, "not a dimension", 0, 0, 0, 0, 0, 0, 1));
		assertInvalid(payload(1L, 0, CASTER, "powers:" + "é".repeat(61), 0, 0, 0, 0, 0, 0, 1));
		assertInvalid(payload(1L, 0, CASTER, "minecraft:overworld", Double.NaN, 0, 0, 0, 0, 0, 1));
		assertInvalid(payload(1L, 0, CASTER, "minecraft:overworld", 30_000_001, 0, 0, 0, 0, 0, 1));
		assertInvalid(payload(1L, 0, CASTER, "minecraft:overworld", 0, 0, 0, Float.NaN, 0, 0, 1));
		assertInvalid(payload(1L, 0, CASTER, "minecraft:overworld", 0, 0, 0, 0, Float.POSITIVE_INFINITY, 0, 1));
		assertInvalid(payload(1L, 0, CASTER, "minecraft:overworld", 0, 0, 0, 0, 0, 2, 1));
		assertInvalid(payload(1L, 0, CASTER, "minecraft:overworld", 0, 0, 0, 0, 0, 0, 0));
		assertInvalid(payload(1L, 0, CASTER, "minecraft:overworld", 0, 0, 0, 0, 0, 0, 81));
	}

	@Test
	void dimensionUtf8BudgetAcceptsExactly128BytesAndRejects129() {
		String exact = "powers:" + "a".repeat(121);
		String oversized = exact + "a";
		var payload = new RankTenSilhouettePackets.Payload(1, 0, CASTER, exact,
				0, 0, 0, 0, 0, 0, 1, 40);
		assertEquals(128, payload.dimension().getBytes(java.nio.charset.StandardCharsets.UTF_8).length);
		ByteBuf bytes = Unpooled.buffer();
		try {
			RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(bytes, RegistryAccess.EMPTY);
			RankTenSilhouettePackets.Payload.STREAM_CODEC.encode(buffer, payload);
			assertEquals(payload, RankTenSilhouettePackets.Payload.STREAM_CODEC.decode(buffer));
		} finally {
			bytes.release();
		}
		assertThrows(IllegalArgumentException.class, () -> new RankTenSilhouettePackets.Payload(
				1, 0, CASTER, oversized, 0, 0, 0, 0, 0, 0, 1, 40));
	}

	@Test
	void decoderRejectsAnOversizedUtf8DimensionBeforeConstruction() {
		ByteBuf bytes = Unpooled.buffer();
		try {
			RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(bytes, RegistryAccess.EMPTY);
			buffer.writeVarLong(1L);
			buffer.writeVarInt(0);
			buffer.writeUUID(CASTER);
			buffer.writeUtf("powers:" + "a".repeat(129), 256);
			buffer.writeDouble(0); buffer.writeDouble(0); buffer.writeDouble(0);
			buffer.writeFloat(0); buffer.writeFloat(0);
			buffer.writeVarInt(0); buffer.writeInt(1); buffer.writeVarInt(40);
			assertThrows(RuntimeException.class,
					() -> RankTenSilhouettePackets.Payload.STREAM_CODEC.decode(buffer));
		} finally {
			bytes.release();
		}
	}

	private static void assertInvalid(ThrowingFactory factory) {
		assertThrows(IllegalArgumentException.class, factory::create);
	}

	private static ThrowingFactory payload(long eventId, int profileId, UUID caster, String dimension,
			double x, double y, double z, float yaw, float pitch, int alignment, int lifetime) {
		return () -> new RankTenSilhouettePackets.Payload(eventId, profileId, caster, dimension,
				x, y, z, yaw, pitch, alignment, 7, lifetime);
	}

	@FunctionalInterface
	private interface ThrowingFactory { Object create(); }
}
