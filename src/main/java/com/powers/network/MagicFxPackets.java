package com.powers.network;

import com.powers.PowersMod;
import com.powers.magic.fx.MagicFxEvent;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** Owns the compact clientbound protocol for semantic magic presentation. */
public final class MagicFxPackets {
	private MagicFxPackets() {
	}

	/** Compact interaction cue; clients generate deterministic geometry locally. */
	public record MagicFxPayload(long eventId, String motif, String sound,
			double x, double y, double z, int primaryColor, int secondaryColor,
			int glyphSeed, int intensity) implements CustomPacketPayload {
		public static final CustomPacketPayload.Type<MagicFxPayload> TYPE =
				new CustomPacketPayload.Type<>(PowersMod.id("magic_fx"));
		public static final StreamCodec<RegistryFriendlyByteBuf, MagicFxPayload> STREAM_CODEC =
				StreamCodec.composite(
						ByteBufCodecs.VAR_LONG, MagicFxPayload::eventId,
						ByteBufCodecs.STRING_UTF8, MagicFxPayload::motif,
						ByteBufCodecs.STRING_UTF8, MagicFxPayload::sound,
						ByteBufCodecs.DOUBLE, MagicFxPayload::x,
						ByteBufCodecs.DOUBLE, MagicFxPayload::y,
						ByteBufCodecs.DOUBLE, MagicFxPayload::z,
						ByteBufCodecs.INT, MagicFxPayload::primaryColor,
						ByteBufCodecs.INT, MagicFxPayload::secondaryColor,
						ByteBufCodecs.INT, MagicFxPayload::glyphSeed,
						ByteBufCodecs.VAR_INT, MagicFxPayload::intensity,
						MagicFxPayload::new);

		public MagicFxPayload(MagicFxEvent event) {
			this(event.eventId(), event.motif(), event.sound(), event.x(), event.y(), event.z(),
					event.primaryColor(), event.secondaryColor(), event.glyphSeed(), event.intensity());
		}

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public static void initialize() {
		PayloadTypeRegistry.clientboundPlay().register(MagicFxPayload.TYPE, MagicFxPayload.STREAM_CODEC);
	}

	/** Sends one semantic cue to nearby clients without shipping particle arrays. */
	public static void broadcast(ServerLevel level, MagicFxEvent event) {
		MagicFxPayload payload = new MagicFxPayload(event);
		for (ServerPlayer observer : level.players()) {
			if (observer.position().distanceToSqr(event.x(), event.y(), event.z()) > 128.0 * 128.0) continue;
			if (ServerPlayNetworking.canSend(observer, MagicFxPayload.TYPE)) ServerPlayNetworking.send(observer, payload);
		}
	}
}
