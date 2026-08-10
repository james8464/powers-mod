package com.powers.network;

import com.powers.PowersMod;
import com.powers.item.ArtifactWeaponManager;
import com.powers.item.artifact.ArtifactAlignment;
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
	public record OpenMenuPayload(String alignment, String selectedKey, int rank, int elementalPhase,
			int sizeMorphOption) implements CustomPacketPayload {
		public static final Type<OpenMenuPayload> TYPE = new Type<>(PowersMod.id("open_shadow_sword"));
		public static final StreamCodec<RegistryFriendlyByteBuf, OpenMenuPayload> STREAM_CODEC =
				StreamCodec.composite(
						ByteBufCodecs.STRING_UTF8, OpenMenuPayload::alignment,
						ByteBufCodecs.STRING_UTF8, OpenMenuPayload::selectedKey,
						ByteBufCodecs.VAR_INT, OpenMenuPayload::rank,
						ByteBufCodecs.VAR_INT, OpenMenuPayload::elementalPhase,
						ByteBufCodecs.VAR_INT, OpenMenuPayload::sizeMorphOption,
						OpenMenuPayload::new);

		@Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
	}

	public record OpenTeleportPayload(String alignment) implements CustomPacketPayload {
		public static final Type<OpenTeleportPayload> TYPE = new Type<>(PowersMod.id("open_shadow_teleport"));
		public static final StreamCodec<RegistryFriendlyByteBuf, OpenTeleportPayload> STREAM_CODEC =
				StreamCodec.composite(ByteBufCodecs.STRING_UTF8, OpenTeleportPayload::alignment,
						OpenTeleportPayload::new);

		@Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
	}

	public record SelectPayload(String alignment, String actionKey, int option) implements CustomPacketPayload {
		public static final Type<SelectPayload> TYPE = new Type<>(PowersMod.id("select_shadow_sword"));
		public static final StreamCodec<RegistryFriendlyByteBuf, SelectPayload> STREAM_CODEC =
				StreamCodec.composite(
						ByteBufCodecs.STRING_UTF8, SelectPayload::alignment,
						ByteBufCodecs.STRING_UTF8, SelectPayload::actionKey,
						ByteBufCodecs.VAR_INT, SelectPayload::option,
						SelectPayload::new);

		@Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
	}

	public record TeleportPayload(String alignment, double x, double y, double z, ResourceKey<Level> dimension,
			String targetName) implements CustomPacketPayload {
		public static final Type<TeleportPayload> TYPE = new Type<>(PowersMod.id("shadow_sword_teleport"));
		public static final StreamCodec<RegistryFriendlyByteBuf, TeleportPayload> STREAM_CODEC =
				StreamCodec.composite(
						ByteBufCodecs.STRING_UTF8, TeleportPayload::alignment,
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
					ArtifactAlignment alignment = parseAlignment(payload.alignment());
					if (alignment != null) ArtifactWeaponManager.select(
							context.player(), alignment, payload.actionKey(), payload.option());
					}
				}));
		ServerPlayNetworking.registerGlobalReceiver(TeleportPayload.TYPE, (payload, context) ->
				context.server().execute(() -> {
					if (PacketRateLimiter.allow(context.player(), PacketRateLimiter.Lane.TRAVEL)) {
						handleTeleport(context.player(), payload);
					}
				}));
	}

	public static void openMenu(ServerPlayer player, ArtifactAlignment alignment,
			String selectedKey, int rank,
			int elementalPhase, int sizeMorphOption) {
		ServerPlayNetworking.send(player, new OpenMenuPayload(
				alignment.serializedName(), selectedKey, rank, elementalPhase, sizeMorphOption));
	}

	public static void openTeleport(ServerPlayer player, ArtifactAlignment alignment) {
		ServerPlayNetworking.send(player, new OpenTeleportPayload(alignment.serializedName()));
	}

	/** Compatibility overload for code paths retained during world migration. */
	public static void openMenu(ServerPlayer player, String selectedKey, int rank,
			int elementalPhase, int sizeMorphOption) {
		openMenu(player, ArtifactAlignment.DARKNESS, selectedKey, rank, elementalPhase, sizeMorphOption);
	}

	/** Compatibility overload for the original Shadow Sword adapter. */
	public static void openTeleport(ServerPlayer player) {
		openTeleport(player, ArtifactAlignment.DARKNESS);
	}

	private static void handleTeleport(ServerPlayer caster, TeleportPayload payload) {
		ArtifactAlignment alignment = parseAlignment(payload.alignment());
		if (alignment == null || !ArtifactWeaponManager.holds(caster, alignment)
				|| !ArtifactWeaponManager.authorized(caster, alignment)
				|| payload.targetName().length() > 16 || !Double.isFinite(payload.x())
				|| !Double.isFinite(payload.y()) || !Double.isFinite(payload.z())) return;
		ArtifactWeaponManager.Action action = ArtifactWeaponManager.selected(caster, alignment);
		if (action == null || !action.ability().requiresInput()
				|| ArtifactWeaponManager.rank(caster, alignment)
				< action.definition().requiredRank()) return;
		ServerPlayer subject = payload.targetName().isBlank() ? caster
				: caster.level().getServer().getPlayerList().getPlayers().stream()
						.filter(player -> player.getName().getString().equalsIgnoreCase(payload.targetName()))
						.findFirst().orElse(null);
		if (subject == null) {
			PowerMessages.send(caster, "powers.packet.player_not_found", 3, payload.targetName());
			return;
		}
		int cooldown = ArtifactWeaponManager.cooldown(caster, alignment, action);
		if (AbilityActivationService.activateArtifactTeleport(caster, subject, action.ability(),
				payload.dimension(), payload.x(), payload.y(), payload.z(), cooldown)
				== AbilityActivationService.Result.ACTIVATED) {
			ArtifactWeaponManager.castFx(caster, alignment, action);
		}
	}

	private static ArtifactAlignment parseAlignment(String value) {
		try {
			return ArtifactAlignment.fromSerialized(value);
		} catch (IllegalArgumentException ignored) {
			return null;
		}
	}
}
