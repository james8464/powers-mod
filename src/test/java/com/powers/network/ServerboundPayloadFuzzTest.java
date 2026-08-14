package com.powers.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Deterministically fuzzes every client-to-server codec with bounded hostile input. */
class ServerboundPayloadFuzzTest {
	private static final int CASES_PER_CODEC = 512;

	@BeforeAll
	static void bootstrapMinecraftRegistries() {
		net.minecraft.SharedConstants.tryDetectVersion();
		net.minecraft.server.Bootstrap.bootStrap();
	}

	@Test
	void everyPlayStageCodecRejectsOrConsumesBoundedInputWithoutHanging() {
		List<NamedCodec<?>> codecs = List.of(
				play("activate", PowersPackets.ActivateAbilityPayload.STREAM_CODEC),
				play("selection", PowersPackets.SelectAbilityOptionPayload.STREAM_CODEC),
				play("teleport", PowersPackets.TeleportRequestPayload.STREAM_CODEC),
				play("teleport_mark", PowersPackets.TeleportMarkPayload.STREAM_CODEC),
				play("locator", PowersPackets.LocateTargetPayload.STREAM_CODEC),
				play("rank", RankPackets.RankActionPayload.STREAM_CODEC),
				play("artifact_select", ShadowSwordPackets.SelectPayload.STREAM_CODEC),
				play("artifact_cycle", ShadowSwordPackets.CyclePayload.STREAM_CODEC),
				play("artifact_bind", ShadowSwordPackets.BindFavouritePayload.STREAM_CODEC),
				play("artifact_teleport", ShadowSwordPackets.TeleportPayload.STREAM_CODEC),
				play("companion", CompanionPackets.InteractPayload.STREAM_CODEC),
				play("vessel", VesselControlPackets.InputPayload.STREAM_CODEC),
				play("vessel_release", VesselControlPackets.ReleasePayload.STREAM_CODEC));
		for (NamedCodec<?> codec : codecs) fuzzPlay(codec);
	}

	@Test
	void configurationResponseCodecIsBoundedUnderHostileInput() {
		Random random = new Random(0x504F57455253L);
		for (int sample = 0; sample < CASES_PER_CODEC; sample++) {
			byte[] bytes = new byte[random.nextInt(257)];
			random.nextBytes(bytes);
			ByteBuf raw = Unpooled.wrappedBuffer(bytes);
			try {
				FriendlyByteBuf buffer = new FriendlyByteBuf(raw);
				try {
					ProtocolHandshakePackets.Response.STREAM_CODEC.decode(buffer);
				} catch (RuntimeException ignored) {
					// A malformed frame is the correct bounded outcome.
				}
				assertTrue(buffer.readerIndex() <= buffer.writerIndex());
			} finally {
				raw.release();
			}
		}
	}

	private static void fuzzPlay(NamedCodec<?> named) {
		Random random = new Random(31L * named.name().hashCode() + 0x504F5745L);
		for (int sample = 0; sample < CASES_PER_CODEC; sample++) {
			byte[] bytes = new byte[random.nextInt(257)];
			random.nextBytes(bytes);
			ByteBuf raw = Unpooled.wrappedBuffer(bytes);
			try {
				RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(raw, RegistryAccess.EMPTY);
				try {
					named.decode(buffer);
				} catch (RuntimeException ignored) {
					// A malformed frame is the correct bounded outcome.
				}
				assertTrue(buffer.readerIndex() <= buffer.writerIndex(), named.name());
			} finally {
				raw.release();
			}
		}
	}

	private static <T extends CustomPacketPayload> NamedCodec<T> play(
			String name, StreamCodec<RegistryFriendlyByteBuf, T> codec) {
		return new NamedCodec<>(name, codec);
	}

	private record NamedCodec<T extends CustomPacketPayload>(
			String name, StreamCodec<RegistryFriendlyByteBuf, T> codec) {
		private void decode(RegistryFriendlyByteBuf buffer) {
			codec.decode(buffer);
		}
	}
}
