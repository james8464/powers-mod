package com.powers.network;

import static org.junit.jupiter.api.Assertions.assertThrows;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.UUID;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Proves text-bearing client requests are bounded at their decode boundary. */
class ServerboundPayloadBoundsTest {
	@BeforeAll
	static void bootstrapMinecraftRegistries() {
		net.minecraft.SharedConstants.tryDetectVersion();
		net.minecraft.server.Bootstrap.bootStrap();
	}

	@Test
	void teleportTargetNamesAreBoundedToAuthoredEntityNameLength() {
		assertRejected(PowersPackets.TeleportRequestPayload.STREAM_CODEC,
				new PowersPackets.TeleportRequestPayload(0, 0, 80, 0,
						net.minecraft.world.level.Level.OVERWORLD, "x".repeat(65), true));
	}

	@Test
	void locatorNamesAreBoundedToAuthoredMobNameLength() {
		assertRejected(PowersPackets.LocateTargetPayload.STREAM_CODEC,
				new PowersPackets.LocateTargetPayload("x".repeat(65), UUID.randomUUID()));
	}

	@Test
	void rankNodeIdentifiersAreBoundedToTheirGrammar() {
		assertRejected(RankPackets.RankActionPayload.STREAM_CODEC,
				new RankPackets.RankActionPayload("x".repeat(49), false));
	}

	private static <T extends CustomPacketPayload> void assertRejected(
			StreamCodec<RegistryFriendlyByteBuf, T> codec, T payload) {
		ByteBuf bytes = Unpooled.buffer();
		try {
			RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(bytes, RegistryAccess.EMPTY);
			assertThrows(RuntimeException.class, () -> codec.encode(buffer, payload));
		} finally {
			bytes.release();
		}
	}
}
