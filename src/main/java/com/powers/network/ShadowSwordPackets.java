package com.powers.network;

import com.powers.PowersMod;
import com.powers.item.ArtifactWeaponManager;
import com.powers.item.artifact.ArtifactScrollRules;
import com.powers.item.artifact.ArtifactFavouriteRules;
import com.powers.item.artifact.ArtifactActionCategory;
import com.powers.item.artifact.ArtifactActionSnapshot;
import com.powers.item.artifact.ArtifactAlignment;
import com.powers.magic.runtime.MagicRuntime;
import com.powers.magic.ActionSubmissionValidation;
import com.powers.item.artifact.ArtifactActionCatalogue;
import com.powers.player.ArtifactSelectionState;
import com.powers.power.AbilityActivationService;
import com.powers.util.PowerMessages;
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
	private static final StreamCodec<io.netty.buffer.ByteBuf, String> ALIGNMENT_CODEC =
			ByteBufCodecs.stringUtf8(16);
	private static final StreamCodec<io.netty.buffer.ByteBuf, String> ACTION_KEY_CODEC =
			ByteBufCodecs.stringUtf8(64);
	private static final StreamCodec<io.netty.buffer.ByteBuf, String> PLAYER_NAME_CODEC =
			ByteBufCodecs.stringUtf8(64);
	private static final StreamCodec<io.netty.buffer.ByteBuf, java.util.List<String>> ACTION_LIST_CODEC =
			ACTION_KEY_CODEC.apply(ByteBufCodecs.list(ArtifactFavouriteRules.SLOT_COUNT));
	private static final StreamCodec<RegistryFriendlyByteBuf, ArtifactActionSnapshot> ACTION_SNAPSHOT_CODEC =
			StreamCodec.of(ShadowSwordPackets::encodeSnapshot, ShadowSwordPackets::decodeSnapshot);
	private static final StreamCodec<RegistryFriendlyByteBuf, java.util.List<ArtifactActionSnapshot>>
			SNAPSHOT_LIST_CODEC = ACTION_SNAPSHOT_CODEC.apply(ByteBufCodecs.list(128));

	public record OpenMenuPayload(long revision, String alignment, String selectedKey, int rank,
			int sizeMorphOption, int energy, java.util.List<String> favourites,
			java.util.List<ArtifactActionSnapshot> actions) implements CustomPacketPayload {
		public static final Type<OpenMenuPayload> TYPE = new Type<>(PowersMod.id("open_shadow_sword"));
		public static final StreamCodec<RegistryFriendlyByteBuf, OpenMenuPayload> STREAM_CODEC =
				StreamCodec.composite(
						ByteBufCodecs.VAR_LONG, OpenMenuPayload::revision,
						ALIGNMENT_CODEC, OpenMenuPayload::alignment,
						ACTION_KEY_CODEC, OpenMenuPayload::selectedKey,
						ByteBufCodecs.VAR_INT, OpenMenuPayload::rank,
						ByteBufCodecs.VAR_INT, OpenMenuPayload::sizeMorphOption,
						ByteBufCodecs.VAR_INT, OpenMenuPayload::energy,
						ACTION_LIST_CODEC, OpenMenuPayload::favourites,
						SNAPSHOT_LIST_CODEC, OpenMenuPayload::actions,
						OpenMenuPayload::new);

		@Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
	}

	public record OpenTeleportPayload(long revision, String alignment, String actionKey) implements CustomPacketPayload {
		public static final Type<OpenTeleportPayload> TYPE = new Type<>(PowersMod.id("open_shadow_teleport"));
		public static final StreamCodec<RegistryFriendlyByteBuf, OpenTeleportPayload> STREAM_CODEC =
				StreamCodec.composite(ByteBufCodecs.VAR_LONG, OpenTeleportPayload::revision,
						ALIGNMENT_CODEC, OpenTeleportPayload::alignment,
						ACTION_KEY_CODEC, OpenTeleportPayload::actionKey,
						OpenTeleportPayload::new);

		@Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
	}

	public record SelectPayload(long revision, String alignment, String actionKey, int option) implements CustomPacketPayload {
		public static final Type<SelectPayload> TYPE = new Type<>(PowersMod.id("select_shadow_sword"));
		public static final StreamCodec<RegistryFriendlyByteBuf, SelectPayload> STREAM_CODEC =
				StreamCodec.composite(
						ByteBufCodecs.VAR_LONG, SelectPayload::revision,
						ALIGNMENT_CODEC, SelectPayload::alignment,
						ACTION_KEY_CODEC, SelectPayload::actionKey,
						ByteBufCodecs.VAR_INT, SelectPayload::option,
						SelectPayload::new);

		@Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
	}

	/** Selects and casts one release-confirmed favourite under server authority. */
	public record CommitPayload(long revision, String alignment, String actionKey, int option)
			implements CustomPacketPayload {
		public static final Type<CommitPayload> TYPE = new Type<>(PowersMod.id("commit_artifact_wheel"));
		public static final StreamCodec<RegistryFriendlyByteBuf, CommitPayload> STREAM_CODEC =
				StreamCodec.composite(ByteBufCodecs.VAR_LONG, CommitPayload::revision,
						ALIGNMENT_CODEC, CommitPayload::alignment,
						ACTION_KEY_CODEC, CommitPayload::actionKey,
						ByteBufCodecs.VAR_INT, CommitPayload::option, CommitPayload::new);

		@Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
	}

	/** One bounded server-authoritative catalogue step requested by crouch-scroll. */
	public record CyclePayload(long revision, String alignment, String actionKey, int direction) implements CustomPacketPayload {
		public static final Type<CyclePayload> TYPE = new Type<>(PowersMod.id("cycle_artifact_action"));
		public static final StreamCodec<RegistryFriendlyByteBuf, CyclePayload> STREAM_CODEC =
				StreamCodec.composite(
						ByteBufCodecs.VAR_LONG, CyclePayload::revision,
						ALIGNMENT_CODEC, CyclePayload::alignment,
						ACTION_KEY_CODEC, CyclePayload::actionKey,
						ByteBufCodecs.VAR_INT, CyclePayload::direction,
						CyclePayload::new);

		@Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
	}

	/** Persists one direct library-to-wheel binding after server validation. */
	public record BindFavouritePayload(long revision, String alignment, int slot, String actionKey)
			implements CustomPacketPayload {
		public static final Type<BindFavouritePayload> TYPE =
				new Type<>(PowersMod.id("bind_artifact_favourite"));
		public static final StreamCodec<RegistryFriendlyByteBuf, BindFavouritePayload> STREAM_CODEC =
				StreamCodec.composite(
						ByteBufCodecs.VAR_LONG, BindFavouritePayload::revision,
						ALIGNMENT_CODEC, BindFavouritePayload::alignment,
						ByteBufCodecs.VAR_INT, BindFavouritePayload::slot,
						ACTION_KEY_CODEC, BindFavouritePayload::actionKey,
						BindFavouritePayload::new);

		@Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
	}

	public record TeleportPayload(long revision, String alignment, String actionKey,
			double x, double y, double z, ResourceKey<Level> dimension,
			String targetName) implements CustomPacketPayload {
		public static final Type<TeleportPayload> TYPE = new Type<>(PowersMod.id("shadow_sword_teleport"));
		public static final StreamCodec<RegistryFriendlyByteBuf, TeleportPayload> STREAM_CODEC =
				StreamCodec.composite(
						ByteBufCodecs.VAR_LONG, TeleportPayload::revision,
						ALIGNMENT_CODEC, TeleportPayload::alignment,
						ACTION_KEY_CODEC, TeleportPayload::actionKey,
						ByteBufCodecs.DOUBLE, TeleportPayload::x,
						ByteBufCodecs.DOUBLE, TeleportPayload::y,
						ByteBufCodecs.DOUBLE, TeleportPayload::z,
						ResourceKey.streamCodec(Registries.DIMENSION), TeleportPayload::dimension,
						PLAYER_NAME_CODEC, TeleportPayload::targetName,
						TeleportPayload::new);

		@Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
	}

	private ShadowSwordPackets() {
	}

	static void initialize() {
		PayloadTypeRegistry.clientboundPlay().register(OpenMenuPayload.TYPE, OpenMenuPayload.STREAM_CODEC);
		PayloadTypeRegistry.clientboundPlay().register(OpenTeleportPayload.TYPE, OpenTeleportPayload.STREAM_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(SelectPayload.TYPE, SelectPayload.STREAM_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(CommitPayload.TYPE, CommitPayload.STREAM_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(CyclePayload.TYPE, CyclePayload.STREAM_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(
				BindFavouritePayload.TYPE, BindFavouritePayload.STREAM_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(TeleportPayload.TYPE, TeleportPayload.STREAM_CODEC);
		ServerPlayNetworking.registerGlobalReceiver(SelectPayload.TYPE, (payload, context) ->
				ServerPlayCallback.execute(context, player -> {
					ArtifactAlignment alignment = parseAlignment(payload.alignment());
					if (!current(payload.revision(), alignment, payload.actionKey())) {
						if (alignment != null) ArtifactWeaponManager.openMenu(player, alignment);
						return;
					}
					if (PacketRateLimiter.allow(player, PacketRateLimiter.Lane.ARTIFACT)) {
					if (alignment != null) ArtifactWeaponManager.select(
							player, alignment, payload.actionKey(), payload.option());
					}
				}));
		ServerPlayNetworking.registerGlobalReceiver(CommitPayload.TYPE, (payload, context) ->
				ServerPlayCallback.execute(context, player -> {
					ArtifactAlignment alignment = parseAlignment(payload.alignment());
					if (!current(payload.revision(), alignment, payload.actionKey())) {
						if (alignment != null) ArtifactWeaponManager.openMenu(player, alignment);
						return;
					}
					if (!PacketRateLimiter.allow(player, PacketRateLimiter.Lane.ARTIFACT)) return;
					if (alignment != null && ArtifactWeaponManager.select(player, alignment,
							payload.actionKey(), payload.option())) {
						ArtifactWeaponManager.activateSelected(player, alignment);
					}
				}));
		ServerPlayNetworking.registerGlobalReceiver(CyclePayload.TYPE, (payload, context) ->
				ServerPlayCallback.execute(context, player -> {
					ArtifactAlignment alignment = parseAlignment(payload.alignment());
					if (!current(payload.revision(), alignment, payload.actionKey())) {
						if (alignment != null) ArtifactWeaponManager.openMenu(player, alignment);
						return;
					}
					if (!PacketRateLimiter.allow(player, PacketRateLimiter.Lane.ARTIFACT)
							|| !ArtifactScrollRules.validDirection(payload.direction())) return;
					if (alignment == null || !ArtifactWeaponManager.holds(player, alignment)
							|| !ArtifactWeaponManager.authorized(player, alignment)) return;
					ArtifactWeaponManager.Action selected = ArtifactWeaponManager.selected(
							player, alignment);
					String next = ArtifactFavouriteRules.cycle(
							ArtifactSelectionState.favourites(player, alignment),
							selected == null ? null : selected.definition().key(), payload.direction());
					if (next != null) ArtifactWeaponManager.select(player, alignment, next, -1);
				}));
		ServerPlayNetworking.registerGlobalReceiver(BindFavouritePayload.TYPE, (payload, context) ->
				ServerPlayCallback.execute(context, player -> {
					ArtifactAlignment alignment = parseAlignment(payload.alignment());
					if (!current(payload.revision(), alignment, payload.actionKey())) {
						if (alignment != null) ArtifactWeaponManager.openMenu(player, alignment);
						return;
					}
					if (!PacketRateLimiter.allow(player, PacketRateLimiter.Lane.ARTIFACT)) return;
					if (alignment == null || !ArtifactWeaponManager.holds(player, alignment)
							|| !ArtifactWeaponManager.authorized(player, alignment)) return;
					ArtifactSelectionState.bindFavourite(player, alignment,
							payload.slot(), payload.actionKey());
				}));
		ServerPlayNetworking.registerGlobalReceiver(TeleportPayload.TYPE, (payload, context) ->
				ServerPlayCallback.execute(context, player -> {
					if (PacketRateLimiter.allow(player, PacketRateLimiter.Lane.TRAVEL)) {
						handleTeleport(player, payload);
					}
				}));
	}

	public static void openMenu(ServerPlayer player, ArtifactAlignment alignment,
			String selectedKey, int rank,
			int sizeMorphOption, int energy,
			java.util.List<String> favourites,
			java.util.List<ArtifactActionSnapshot> actions) {
		ServerPlayNetworking.send(player, new OpenMenuPayload(
				MagicRuntime.catalogue().snapshot().revision(), alignment.serializedName(), selectedKey, rank, sizeMorphOption,
				energy, java.util.List.copyOf(favourites), java.util.List.copyOf(actions)));
	}

	private static boolean current(long revision, ArtifactAlignment alignment, String actionKey) {
		var action = alignment == null ? null : ArtifactActionCatalogue.find(alignment, actionKey);
		return action != null && ActionSubmissionValidation.validate(
				MagicRuntime.catalogue().snapshot(), revision, action.abilityId())
				== ActionSubmissionValidation.ACCEPT;
	}

	public static void openTeleport(ServerPlayer player, ArtifactAlignment alignment) {
		ArtifactWeaponManager.Action selected = ArtifactWeaponManager.selected(player, alignment);
		if (selected == null) return;
		ServerPlayNetworking.send(player, new OpenTeleportPayload(
				MagicRuntime.catalogue().snapshot().revision(), alignment.serializedName(),
				selected.definition().key()));
	}

	private static void handleTeleport(ServerPlayer caster, TeleportPayload payload) {
		ArtifactAlignment alignment = parseAlignment(payload.alignment());
		if (!current(payload.revision(), alignment, payload.actionKey())) {
			if (alignment != null) ArtifactWeaponManager.openMenu(caster, alignment);
			return;
		}
		if (alignment == null || !ArtifactWeaponManager.holds(caster, alignment)
				|| !ArtifactWeaponManager.authorized(caster, alignment)
				|| payload.targetName().length() > 64 || !Double.isFinite(payload.x())
				|| !Double.isFinite(payload.y()) || !Double.isFinite(payload.z())) return;
		ArtifactWeaponManager.Action action = ArtifactWeaponManager.selected(caster, alignment);
		if (action == null || !action.definition().key().equals(payload.actionKey())
				|| !action.ability().requiresInput()
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

	private static void encodeSnapshot(RegistryFriendlyByteBuf buffer, ArtifactActionSnapshot snapshot) {
		ACTION_KEY_CODEC.encode(buffer, snapshot.key());
		buffer.writeVarInt(snapshot.category().ordinal());
		buffer.writeVarInt(snapshot.cost());
		buffer.writeVarInt(snapshot.energySaved());
		buffer.writeVarInt(snapshot.cooldownTicks());
		buffer.writeVarInt(snapshot.cooldownMaximumTicks());
		buffer.writeBoolean(snapshot.active());
		buffer.writeBoolean(snapshot.locked());
		buffer.writeVarInt(snapshot.variant());
	}

	private static ArtifactActionSnapshot decodeSnapshot(RegistryFriendlyByteBuf buffer) {
		String key = ACTION_KEY_CODEC.decode(buffer);
		int categoryIndex = buffer.readVarInt();
		ArtifactActionCategory[] categories = ArtifactActionCategory.values();
		ArtifactActionCategory category = categories[Math.clamp(categoryIndex, 0, categories.length - 1)];
		return new ArtifactActionSnapshot(key, category, buffer.readVarInt(), buffer.readVarInt(),
				buffer.readVarInt(), buffer.readVarInt(), buffer.readBoolean(), buffer.readBoolean(),
				buffer.readVarInt());
	}
}
