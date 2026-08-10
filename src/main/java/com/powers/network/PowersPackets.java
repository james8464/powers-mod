package com.powers.network;

import com.powers.PowersMod;
import com.powers.player.PlayerPowers;
import com.powers.player.SkillSystem;
import com.powers.power.Ability;
import com.powers.power.AbilityActivationService;
import com.powers.power.Power;
import com.powers.power.ActivationCooldowns;
import com.powers.power.AmethystDampening;
import com.powers.power.crystals.SpaceTimeAbility;
import com.powers.power.state.GlobalTimeStopManager;
import com.powers.power.abilities.TeleportAbility;
import com.powers.util.PowerMessages;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.HashMap;
import java.util.Map;

/**
 * Declares the mod's play payloads and validates server-authoritative power
 * activation, teleport selection, and client state synchronization.
 */
public final class PowersPackets {
	private static final Map<UUID, PowerStatePayload> LAST_SENT_STATE = new HashMap<>();
	private PowersPackets() {
	}

	public record ActivateAbilityPayload(int slot) implements CustomPacketPayload {
		public static final CustomPacketPayload.Type<ActivateAbilityPayload> TYPE =
				new CustomPacketPayload.Type<>(PowersMod.id("activate_ability"));
		public static final StreamCodec<RegistryFriendlyByteBuf, ActivateAbilityPayload> STREAM_CODEC =
				StreamCodec.composite(
						ByteBufCodecs.VAR_INT, ActivateAbilityPayload::slot,
						ActivateAbilityPayload::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** Server-validated free selection for a power with authored menu options. */
	public record SelectAbilityOptionPayload(int slot, int option) implements CustomPacketPayload {
		public static final CustomPacketPayload.Type<SelectAbilityOptionPayload> TYPE =
				new CustomPacketPayload.Type<>(PowersMod.id("select_ability_option"));
		public static final StreamCodec<RegistryFriendlyByteBuf, SelectAbilityOptionPayload> STREAM_CODEC =
				StreamCodec.composite(
						ByteBufCodecs.VAR_INT, SelectAbilityOptionPayload::slot,
						ByteBufCodecs.VAR_INT, SelectAbilityOptionPayload::option,
						SelectAbilityOptionPayload::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record TeleportRequestPayload(int slot, double x, double y, double z,
			ResourceKey<Level> dimension, String targetName, boolean toPlayer) implements CustomPacketPayload {
		public static final CustomPacketPayload.Type<TeleportRequestPayload> TYPE =
				new CustomPacketPayload.Type<>(PowersMod.id("teleport_request"));
		public static final StreamCodec<RegistryFriendlyByteBuf, TeleportRequestPayload> STREAM_CODEC =
				StreamCodec.composite(
						ByteBufCodecs.VAR_INT, TeleportRequestPayload::slot,
						ByteBufCodecs.DOUBLE, TeleportRequestPayload::x,
						ByteBufCodecs.DOUBLE, TeleportRequestPayload::y,
						ByteBufCodecs.DOUBLE, TeleportRequestPayload::z,
						ResourceKey.streamCodec(Registries.DIMENSION), TeleportRequestPayload::dimension,
						ByteBufCodecs.STRING_UTF8, TeleportRequestPayload::targetName,
						ByteBufCodecs.BOOL, TeleportRequestPayload::toPlayer,
						TeleportRequestPayload::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record TeleportMarkPayload(int slot, double x, double y, double z) implements CustomPacketPayload {
		public static final CustomPacketPayload.Type<TeleportMarkPayload> TYPE =
				new CustomPacketPayload.Type<>(PowersMod.id("teleport_mark"));
		public static final StreamCodec<RegistryFriendlyByteBuf, TeleportMarkPayload> STREAM_CODEC =
				StreamCodec.composite(
						ByteBufCodecs.VAR_INT, TeleportMarkPayload::slot,
						ByteBufCodecs.DOUBLE, TeleportMarkPayload::x,
						ByteBufCodecs.DOUBLE, TeleportMarkPayload::y,
						ByteBufCodecs.DOUBLE, TeleportMarkPayload::z,
						TeleportMarkPayload::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	// the server's go-ahead for the celestial grimoire: open the locator screen
	public record OpenLocatorScreenPayload(UUID nonce) implements CustomPacketPayload {
		public static final CustomPacketPayload.Type<OpenLocatorScreenPayload> TYPE =
				new CustomPacketPayload.Type<>(PowersMod.id("open_locator"));
		public static final StreamCodec<RegistryFriendlyByteBuf, OpenLocatorScreenPayload> STREAM_CODEC =
				StreamCodec.composite(UUID_CODEC, OpenLocatorScreenPayload::nonce, OpenLocatorScreenPayload::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** Names an online player or uniquely custom-named loaded mob for remote viewing. */
	public record LocateTargetPayload(String targetName, UUID nonce) implements CustomPacketPayload {
		public static final CustomPacketPayload.Type<LocateTargetPayload> TYPE =
				new CustomPacketPayload.Type<>(PowersMod.id("locate_target"));
		public static final StreamCodec<RegistryFriendlyByteBuf, LocateTargetPayload> STREAM_CODEC =
				StreamCodec.composite(
						ByteBufCodecs.STRING_UTF8, LocateTargetPayload::targetName,
						UUID_CODEC, LocateTargetPayload::nonce,
						LocateTargetPayload::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public static void openLocator(ServerPlayer player) {
		LocatorSpellPackets.open(player);
	}

	private static final StreamCodec<RegistryFriendlyByteBuf, UUID> UUID_CODEC = StreamCodec.of(
			(buf, uuid) -> {
				buf.writeLong(uuid.getMostSignificantBits());
				buf.writeLong(uuid.getLeastSignificantBits());
			},
			buf -> new UUID(buf.readLong(), buf.readLong()));

	public static void initialize() {
		PayloadTypeRegistry.serverboundPlay().register(ActivateAbilityPayload.TYPE, ActivateAbilityPayload.STREAM_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(
				SelectAbilityOptionPayload.TYPE, SelectAbilityOptionPayload.STREAM_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(TeleportRequestPayload.TYPE, TeleportRequestPayload.STREAM_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(TeleportMarkPayload.TYPE, TeleportMarkPayload.STREAM_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(LocateTargetPayload.TYPE, LocateTargetPayload.STREAM_CODEC);
		PayloadTypeRegistry.clientboundPlay().register(PowerStatePayload.TYPE, PowerStatePayload.STREAM_CODEC);
		PayloadTypeRegistry.clientboundPlay().register(OpenLocatorScreenPayload.TYPE, OpenLocatorScreenPayload.STREAM_CODEC);
		RankPackets.initialize();
		MagicFxPackets.initialize();
		BodyProxyPackets.initialize();
		ShadowSwordPackets.initialize();

		ServerPlayNetworking.registerGlobalReceiver(ActivateAbilityPayload.TYPE, PowersPackets::handleActivate);
		ServerPlayNetworking.registerGlobalReceiver(
				SelectAbilityOptionPayload.TYPE, PowersPackets::handleSelection);
		ServerPlayNetworking.registerGlobalReceiver(TeleportRequestPayload.TYPE, PowersPackets::handleTeleport);
		ServerPlayNetworking.registerGlobalReceiver(TeleportMarkPayload.TYPE, PowersPackets::handleMark);
		ServerPlayNetworking.registerGlobalReceiver(LocateTargetPayload.TYPE, LocatorSpellPackets::handleLocate);
	}

	private static void handleActivate(ActivateAbilityPayload payload, ServerPlayNetworking.Context context) {
		context.server().execute(() -> {
			ServerPlayer player = context.player();
			if (!PacketRateLimiter.allow(player, PacketRateLimiter.Lane.ACTIVATION)) return;
			if (payload.slot() < 0 || payload.slot() >= PlayerPowers.SLOT_COUNT) return;
			PlayerPowers.PlayerPowersData data = PlayerPowers.get(player);
			Power power = data.getPower(payload.slot());
			if (power == null) return;
			Ability ability = power.ability();
			if (ability != null && !ability.requiresInput()) {
				AbilityActivationService.activate(player, ability, power.id().toString());
			}
		});
	}

	private static void handleSelection(SelectAbilityOptionPayload payload,
			ServerPlayNetworking.Context context) {
		context.server().execute(() -> {
			ServerPlayer player = context.player();
			if (!PacketRateLimiter.allow(player, PacketRateLimiter.Lane.SELECTION)) return;
			if (GlobalTimeStopManager.rejectIfStopped(player)) return;
			AmethystDampening.update(player);
			if (AmethystDampening.isDampened(player)) {
				AmethystDampening.punish(player);
				return;
			}
			if (SpaceTimeAbility.isFrozen(player)) {
				SpaceTimeAbility.reject(player);
				return;
			}
			if (payload.slot() < 0 || payload.slot() >= PlayerPowers.SLOT_COUNT) return;
			PlayerPowers.PlayerPowersData data = PlayerPowers.get(player);
			Power power = data.getPower(payload.slot());
			Ability ability = power == null ? null : power.ability();
			if (ability == null || payload.option() < 0
					|| payload.option() >= ability.selectionOptionCount()) return;
			if (ability.selectOption(player, data, payload.option())) syncTo(player);
		});
	}

	private static void handleTeleport(TeleportRequestPayload payload, ServerPlayNetworking.Context context) {
		context.server().execute(() -> {
			ServerPlayer player = context.player();
			if (!PacketRateLimiter.allow(player, PacketRateLimiter.Lane.TRAVEL)) return;
			PlayerPowers.PlayerPowersData data = PlayerPowers.get(player);
			// guards against malformed packets: names cap at 16 chars and
			// NaN coordinates must never reach the teleport code
			if (payload.slot() < 0 || payload.slot() >= PlayerPowers.SLOT_COUNT
					|| payload.targetName().length() > 16
					|| !Double.isFinite(payload.x()) || !Double.isFinite(payload.y()) || !Double.isFinite(payload.z())) return;
			Power power = data.getPower(payload.slot());
			// only targeted teleports arrive here; one-shot casts come through activate
			if (power == null || power.ability() == null || !power.ability().requiresInput()) return;
			Ability ability = power.ability();

			if (payload.toPlayer()) {
				// warping to a player drops you next to them in marking mode (spectator) to pick the exact landing spot
				ServerPlayer target = findPlayer(player, payload.targetName());
				if (target == null) return;
				AbilityActivationService.activateInput(player, ability, false,
						() -> TeleportAbility.startMarking(player, target, payload.slot()));
				return;
			}

			ServerPlayer subject = payload.targetName().isEmpty()
					? player : findPlayer(player, payload.targetName());
			if (subject == null) return;
			AbilityActivationService.activateTeleport(player, subject, ability, payload.dimension(),
					payload.x(), payload.y(), payload.z(), false);
		});
	}

	private static void handleMark(TeleportMarkPayload payload, ServerPlayNetworking.Context context) {
		context.server().execute(() -> {
			ServerPlayer player = context.player();
			if (!PacketRateLimiter.allow(player, PacketRateLimiter.Lane.TRAVEL)) return;
			if (GlobalTimeStopManager.rejectIfStopped(player)) return;
			if (payload.slot() < 0 || payload.slot() >= PlayerPowers.SLOT_COUNT) return;
			// reject garbage: NaN coordinates would corrupt the stored mark
			if (!Double.isFinite(payload.x()) || !Double.isFinite(payload.y()) || !Double.isFinite(payload.z())) return;
			AmethystDampening.update(player);
			// the same counterplay applies to marking a teleport spot
			if (AmethystDampening.isDampened(player)) {
				TeleportAbility.clearMarking(player);
				AmethystDampening.punish(player);
				return;
			}
			TeleportAbility.completeMarking(player, payload.slot(),
					new Vec3(payload.x(), payload.y(), payload.z()));
		});
	}

	private static ServerPlayer findPlayer(ServerPlayer caster, String name) {
		for (ServerPlayer p : ((net.minecraft.server.level.ServerLevel) caster.level()).getServer().getPlayerList().getPlayers()) {
			if (p.getName().getString().equalsIgnoreCase(name)) {
				return p;
			}
		}
		PowerMessages.send(caster, "powers.packet.player_not_found", 3, name);
		return null;
	}

	// sends the player's current power state so the client HUD matches the server
	public static void syncTo(ServerPlayer player) {
		PlayerPowers.PlayerPowersData data = PlayerPowers.get(player);
		boolean darkness = data.isDarknessUser();
		var rankProgress = data.rankProgress(darkness);
		List<Integer> cooldowns = new ArrayList<>();
		List<Integer> cooldownMaximums = new ArrayList<>();
		List<Integer> reactivations = new ArrayList<>();
		for (int slot = 0; slot < PlayerPowers.SLOT_COUNT; slot++) {
			Power power = data.getPower(slot);
			Ability ability = power == null ? null : power.ability();
			cooldowns.add(ability == null || ability.isToggle()
					? 0 : ActivationCooldowns.remainingTicks(player, ability));
			cooldownMaximums.add(ability == null || ability.isToggle()
					? 0 : ability.cooldownTicksFor(player, data));
			reactivations.add(ability == null || ability.isToggle()
					? 0 : Math.max(0, ability.reactivationTicks(player, data)));
		}
		PowerStatePayload payload = new PowerStatePayload(
				data.getSlotIds(),
				data.getActiveToggles(),
				cooldowns,
				cooldownMaximums,
				reactivations,
				data.energy(),
				data.energyCapacity(),
				SkillSystem.canEnterDarkRealm(player),
				darkness,
				data.mindBody() != null,
				data.getPhase(),
				data.getSizeMorphOption(),
				rankProgress.completed().stream().sorted().toList(),
				rankProgress.focus(),
				darkness ? data.darknessLevel() : data.skillLevel());
		if (payload.equals(LAST_SENT_STATE.get(player.getUUID()))) return;
		LAST_SENT_STATE.put(player.getUUID(), payload);
		ServerPlayNetworking.send(player, payload);
	}

	public static void forget(ServerPlayer player) {
		LAST_SENT_STATE.remove(player.getUUID());
		PacketRateLimiter.forgetPlayer(player.getUUID());
	}

	public static void clearSyncCache() {
		LAST_SENT_STATE.clear();
		PacketRateLimiter.clearGlobal();
	}
}
