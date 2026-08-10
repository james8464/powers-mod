package com.powers.network;

import com.powers.PowersMod;
import com.powers.item.ShadowSwordPowerManager;
import com.powers.item.ShadowSwordRules;
import com.powers.player.PlayerPowers;
import com.powers.power.AbilityActivationService;
import com.powers.util.PowerMessages;
import com.powers.fx.ShadowSwordFx;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/** Dedicated payload surface for the Shadow Sword menu and its input ability. */
public final class ShadowSwordPackets {
	public record OpenMenuPayload(String selectedKey, int darknessLevel, int elementalPhase,
			int sizeMorphOption) implements CustomPacketPayload {
		public static final Type<OpenMenuPayload> TYPE = new Type<>(PowersMod.id("open_shadow_sword"));
		public static final StreamCodec<RegistryFriendlyByteBuf, OpenMenuPayload> STREAM_CODEC =
				StreamCodec.composite(
						ByteBufCodecs.STRING_UTF8, OpenMenuPayload::selectedKey,
						ByteBufCodecs.VAR_INT, OpenMenuPayload::darknessLevel,
						ByteBufCodecs.VAR_INT, OpenMenuPayload::elementalPhase,
						ByteBufCodecs.VAR_INT, OpenMenuPayload::sizeMorphOption,
						OpenMenuPayload::new);

		@Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
	}

	public record OpenTeleportPayload() implements CustomPacketPayload {
		public static final Type<OpenTeleportPayload> TYPE = new Type<>(PowersMod.id("open_shadow_teleport"));
		public static final StreamCodec<RegistryFriendlyByteBuf, OpenTeleportPayload> STREAM_CODEC =
				StreamCodec.unit(new OpenTeleportPayload());

		@Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
	}

	public record SelectPayload(String actionKey, int option) implements CustomPacketPayload {
		public static final Type<SelectPayload> TYPE = new Type<>(PowersMod.id("select_shadow_sword"));
		public static final StreamCodec<RegistryFriendlyByteBuf, SelectPayload> STREAM_CODEC =
				StreamCodec.composite(
						ByteBufCodecs.STRING_UTF8, SelectPayload::actionKey,
						ByteBufCodecs.VAR_INT, SelectPayload::option,
						SelectPayload::new);

		@Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
	}

	public record TeleportPayload(double x, double y, double z, ResourceKey<Level> dimension,
			String targetName) implements CustomPacketPayload {
		public static final Type<TeleportPayload> TYPE = new Type<>(PowersMod.id("shadow_sword_teleport"));
		public static final StreamCodec<RegistryFriendlyByteBuf, TeleportPayload> STREAM_CODEC =
				StreamCodec.composite(
						ByteBufCodecs.DOUBLE, TeleportPayload::x,
						ByteBufCodecs.DOUBLE, TeleportPayload::y,
						ByteBufCodecs.DOUBLE, TeleportPayload::z,
						ResourceKey.streamCodec(Registries.DIMENSION), TeleportPayload::dimension,
						ByteBufCodecs.STRING_UTF8, TeleportPayload::targetName,
						TeleportPayload::new);

		@Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
	}

	private ShadowSwordPackets() {
	}

	static void initialize() {
		PayloadTypeRegistry.clientboundPlay().register(OpenMenuPayload.TYPE, OpenMenuPayload.STREAM_CODEC);
		PayloadTypeRegistry.clientboundPlay().register(OpenTeleportPayload.TYPE, OpenTeleportPayload.STREAM_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(SelectPayload.TYPE, SelectPayload.STREAM_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(TeleportPayload.TYPE, TeleportPayload.STREAM_CODEC);
		ServerPlayNetworking.registerGlobalReceiver(SelectPayload.TYPE, (payload, context) ->
				context.server().execute(() -> {
					if (PacketRateLimiter.allow(context.player(), PacketRateLimiter.Lane.ARTIFACT)) {
						ShadowSwordPowerManager.select(context.player(), payload.actionKey(), payload.option());
					}
				}));
		ServerPlayNetworking.registerGlobalReceiver(TeleportPayload.TYPE, (payload, context) ->
				context.server().execute(() -> {
					if (PacketRateLimiter.allow(context.player(), PacketRateLimiter.Lane.TRAVEL)) {
						handleTeleport(context.player(), payload);
					}
				}));
	}

	public static void openMenu(ServerPlayer player, String selectedKey, int darknessLevel,
			int elementalPhase, int sizeMorphOption) {
		ServerPlayNetworking.send(player, new OpenMenuPayload(
				selectedKey, darknessLevel, elementalPhase, sizeMorphOption));
	}

	public static void openTeleport(ServerPlayer player) {
		ServerPlayNetworking.send(player, new OpenTeleportPayload());
	}

	private static void handleTeleport(ServerPlayer caster, TeleportPayload payload) {
		if (!ShadowSwordPowerManager.holdsSword(caster) || !ShadowSwordPowerManager.authorized(caster)
				|| payload.targetName().length() > 16 || !Double.isFinite(payload.x())
				|| !Double.isFinite(payload.y()) || !Double.isFinite(payload.z())) return;
		ShadowSwordPowerManager.Action action = ShadowSwordPowerManager.selected(caster);
		if (action == null || !action.ability().requiresInput()
				|| !ShadowSwordPowerManager.unlocked(caster, action)) return;
		ServerPlayer subject = payload.targetName().isBlank() ? caster
				: caster.level().getServer().getPlayerList().getPlayers().stream()
						.filter(player -> player.getName().getString().equalsIgnoreCase(payload.targetName()))
						.findFirst().orElse(null);
		if (subject == null) {
			PowerMessages.send(caster, "powers.packet.player_not_found", 3, payload.targetName());
			return;
		}
		boolean apotheosis = ShadowSwordRules.bypassesCooldown(PlayerPowers.get(caster).darknessLevel());
		if (AbilityActivationService.activateTeleport(caster, subject, action.ability(), payload.dimension(),
				payload.x(), payload.y(), payload.z(), apotheosis)
				== AbilityActivationService.Result.ACTIVATED) {
			ShadowSwordFx.corruptedCast((net.minecraft.server.level.ServerLevel) caster.level(),
					caster.position(), action.definition().key().hashCode(), 0x45205A);
		}
	}
}
