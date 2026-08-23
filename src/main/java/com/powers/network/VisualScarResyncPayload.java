package com.powers.network;

import com.powers.PowersMod;
import com.powers.fx.VisualScarService;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Authenticated bounded request for a fresh authoritative scar reset and snapshot. */
public record VisualScarResyncPayload() implements CustomPacketPayload {
	public static final Type<VisualScarResyncPayload> TYPE =
			new Type<>(PowersMod.id("scar_resync"));
	public static final StreamCodec<RegistryFriendlyByteBuf, VisualScarResyncPayload> STREAM_CODEC =
			StreamCodec.of((buffer, payload) -> { }, buffer -> new VisualScarResyncPayload());

	static void initialize() {
		PayloadTypeRegistry.serverboundPlay().register(TYPE, STREAM_CODEC);
		PowersPlayNetworking.registerReceiver(TYPE,
				(payload, player) -> VisualScarService.requestResync(player));
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
