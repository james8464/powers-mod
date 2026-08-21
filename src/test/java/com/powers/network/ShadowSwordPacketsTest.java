package com.powers.network;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.powers.item.artifact.ArtifactAlignment;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

/** Guards the serverbound artifact menu boundary against oversized text. */
class ShadowSwordPacketsTest {
	@BeforeAll
	static void bootstrapMinecraftRegistries() {
		net.minecraft.SharedConstants.tryDetectVersion();
		net.minecraft.server.Bootstrap.bootStrap();
	}

	@Test
	void oversizedActionKeysAreRejectedDuringEncoding() {
		ByteBuf bytes = Unpooled.buffer();
		try {
			RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(bytes, RegistryAccess.EMPTY);
			ShadowSwordPackets.SelectPayload payload = new ShadowSwordPackets.SelectPayload(
					7L, ArtifactAlignment.DARKNESS.serializedName(), "x".repeat(65), 0);

			assertThrows(RuntimeException.class,
					() -> ShadowSwordPackets.SelectPayload.STREAM_CODEC.encode(buffer, payload));
		} finally {
			bytes.release();
		}
	}

	@Test
	void oversizedTargetNamesAreRejectedDuringEncoding() {
		ByteBuf bytes = Unpooled.buffer();
		try {
			RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(bytes, RegistryAccess.EMPTY);
			ShadowSwordPackets.TeleportPayload payload = new ShadowSwordPackets.TeleportPayload(
					7L, ArtifactAlignment.DARKNESS.serializedName(), "innate/time_shift", 0, 80, 0,
					net.minecraft.world.level.Level.OVERWORLD, "x".repeat(65));

			assertThrows(RuntimeException.class,
					() -> ShadowSwordPackets.TeleportPayload.STREAM_CODEC.encode(buffer, payload));
		} finally {
			bytes.release();
		}
	}

	@Test
	void menuSnapshotRoundTripsBoundedRecentCanonicalKeys() {
		ByteBuf bytes = Unpooled.buffer();
		try {
			RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(bytes, RegistryAccess.EMPTY);
			ShadowSwordPackets.OpenMenuPayload expected = new ShadowSwordPackets.OpenMenuPayload(
					11L, "darkness", "innate/fireball", 10, 3, 100,
					List.of("innate/fireball"), List.of("innate/fireball", "crystal/inferno"), List.of());
			ShadowSwordPackets.OpenMenuPayload.STREAM_CODEC.encode(buffer, expected);
			assertEquals(expected, ShadowSwordPackets.OpenMenuPayload.STREAM_CODEC.decode(buffer));
		} finally {
			bytes.release();
		}
	}
}
