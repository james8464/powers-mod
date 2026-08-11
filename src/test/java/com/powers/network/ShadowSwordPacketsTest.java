package com.powers.network;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.powers.item.artifact.ArtifactAlignment;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

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
					ArtifactAlignment.DARKNESS.serializedName(), "x".repeat(65), 0);

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
					ArtifactAlignment.DARKNESS.serializedName(), 0, 80, 0,
					net.minecraft.world.level.Level.OVERWORLD, "x".repeat(65));

			assertThrows(RuntimeException.class,
					() -> ShadowSwordPackets.TeleportPayload.STREAM_CODEC.encode(buffer, payload));
		} finally {
			bytes.release();
		}
	}
}
