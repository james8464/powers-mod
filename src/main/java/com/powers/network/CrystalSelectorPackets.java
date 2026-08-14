package com.powers.network;

import com.powers.PowersItems;
import com.powers.PowersMod;
import com.powers.power.Ability;
import com.powers.power.crystals.CrystalPowerRegistry;
import com.powers.power.crystals.ModeCrystalAbility;
import com.powers.magic.runtime.MagicRuntime;
import com.powers.magic.ActionSubmissionValidation;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/** Authenticated Rainbow convergence selector transport. */
public final class CrystalSelectorPackets {
	private static final StreamCodec<io.netty.buffer.ByteBuf, String> MODE_CODEC =
			ByteBufCodecs.stringUtf8(64);

	public record OpenPayload(long revision, List<String> modes, int selected) implements CustomPacketPayload {
		public static final Type<OpenPayload> TYPE = new Type<>(PowersMod.id("open_crystal_selector"));
		public static final StreamCodec<RegistryFriendlyByteBuf, OpenPayload> STREAM_CODEC =
				StreamCodec.composite(ByteBufCodecs.VAR_LONG, OpenPayload::revision,
						MODE_CODEC.apply(ByteBufCodecs.list(8)), OpenPayload::modes,
						ByteBufCodecs.VAR_INT, OpenPayload::selected, OpenPayload::new);
		@Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
	}

	public record SelectPayload(long revision, String actionKey) implements CustomPacketPayload {
		public static final Type<SelectPayload> TYPE = new Type<>(PowersMod.id("select_crystal_mode"));
		public static final StreamCodec<RegistryFriendlyByteBuf, SelectPayload> STREAM_CODEC =
				StreamCodec.composite(ByteBufCodecs.VAR_LONG, SelectPayload::revision,
						MODE_CODEC, SelectPayload::actionKey, SelectPayload::new);
		@Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
	}

	private CrystalSelectorPackets() {
	}

	static void initialize() {
		PayloadTypeRegistry.clientboundPlay().register(OpenPayload.TYPE, OpenPayload.STREAM_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(SelectPayload.TYPE, SelectPayload.STREAM_CODEC);
		ServerPlayNetworking.registerGlobalReceiver(SelectPayload.TYPE, (payload, context) ->
				ServerPlayCallback.execute(context, player -> {
					if (PacketRateLimiter.allow(player, PacketRateLimiter.Lane.ARTIFACT)) {
						select(player, payload);
					}
				}));
	}

	public static void open(ServerPlayer player, ModeCrystalAbility convergence) {
		ServerPlayNetworking.send(player,
				new OpenPayload(MagicRuntime.catalogue().snapshot().revision(),
						convergence.modeIds(), convergence.selectedIndex(player)));
	}

	private static void select(ServerPlayer player, SelectPayload payload) {
		boolean holds = player.getMainHandItem().is(PowersItems.RAINBOW_CRYSTAL)
				|| player.getOffhandItem().is(PowersItems.RAINBOW_CRYSTAL);
		Ability ability = CrystalPowerRegistry.get(PowersItems.RAINBOW_CRYSTAL);
		if (holds && ability instanceof ModeCrystalAbility convergence && convergence.radialSelector()) {
			if (ActionSubmissionValidation.validate(MagicRuntime.catalogue().snapshot(),
					payload.revision(), payload.actionKey()) != ActionSubmissionValidation.ACCEPT) {
				open(player, convergence);
				return;
			}
			int selected = convergence.modeIds().indexOf(payload.actionKey());
			if (selected >= 0) convergence.selectMode(player, selected);
		}
	}
}
