package com.powers.network;

import com.powers.PowersMod;
import com.powers.player.PlayerPowers;
import com.powers.power.Ability;
import com.powers.power.Power;
import com.powers.power.PowerEnergy;
import com.powers.power.AmethystDampening;
import com.powers.power.crystals.SpaceTimeAbility;
import com.powers.power.abilities.TeleportAbility;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public final class PowersPackets {
	private PowersPackets() {
	}

	public record ActivateAbilityPayload(int slot, int abilityIndex) implements CustomPacketPayload {
		public static final CustomPacketPayload.Type<ActivateAbilityPayload> TYPE =
				new CustomPacketPayload.Type<>(PowersMod.id("activate_ability"));
		public static final StreamCodec<RegistryFriendlyByteBuf, ActivateAbilityPayload> STREAM_CODEC =
				StreamCodec.composite(
						ByteBufCodecs.VAR_INT, ActivateAbilityPayload::slot,
						ByteBufCodecs.VAR_INT, ActivateAbilityPayload::abilityIndex,
						ActivateAbilityPayload::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record TeleportRequestPayload(int slot, int abilityIndex, double x, double y, double z,
			ResourceKey<Level> dimension, String targetName, boolean toPlayer) implements CustomPacketPayload {
		public static final CustomPacketPayload.Type<TeleportRequestPayload> TYPE =
				new CustomPacketPayload.Type<>(PowersMod.id("teleport_request"));
		public static final StreamCodec<RegistryFriendlyByteBuf, TeleportRequestPayload> STREAM_CODEC =
				StreamCodec.composite(
						ByteBufCodecs.VAR_INT, TeleportRequestPayload::slot,
						ByteBufCodecs.VAR_INT, TeleportRequestPayload::abilityIndex,
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

	public record SetPowerSlotsPayload(List<String> powerIds) implements CustomPacketPayload {
		public static final CustomPacketPayload.Type<SetPowerSlotsPayload> TYPE =
				new CustomPacketPayload.Type<>(PowersMod.id("set_power_slots"));
		public static final StreamCodec<RegistryFriendlyByteBuf, SetPowerSlotsPayload> STREAM_CODEC =
				StreamCodec.composite(
						ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.STRING_UTF8),
						SetPowerSlotsPayload::powerIds,
						SetPowerSlotsPayload::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record RerollPowerSlotsPayload(int token) implements CustomPacketPayload {
		public static final CustomPacketPayload.Type<RerollPowerSlotsPayload> TYPE =
				new CustomPacketPayload.Type<>(PowersMod.id("reroll_power_slots"));
		public static final StreamCodec<RegistryFriendlyByteBuf, RerollPowerSlotsPayload> STREAM_CODEC =
				StreamCodec.composite(ByteBufCodecs.VAR_INT, RerollPowerSlotsPayload::token, RerollPowerSlotsPayload::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record PowerStatePayload(List<String> powerIds, List<String> activeToggles, int energy) implements CustomPacketPayload {
		public static final CustomPacketPayload.Type<PowerStatePayload> TYPE =
				new CustomPacketPayload.Type<>(PowersMod.id("power_state"));
		public static final StreamCodec<RegistryFriendlyByteBuf, PowerStatePayload> STREAM_CODEC =
				StreamCodec.composite(
						ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.STRING_UTF8),
						PowerStatePayload::powerIds,
						ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.STRING_UTF8),
						PowerStatePayload::activeToggles,
						ByteBufCodecs.VAR_INT,
						PowerStatePayload::energy,
						PowerStatePayload::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public static void initialize() {
		PayloadTypeRegistry.serverboundPlay().register(ActivateAbilityPayload.TYPE, ActivateAbilityPayload.STREAM_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(TeleportRequestPayload.TYPE, TeleportRequestPayload.STREAM_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(TeleportMarkPayload.TYPE, TeleportMarkPayload.STREAM_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(SetPowerSlotsPayload.TYPE, SetPowerSlotsPayload.STREAM_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(RerollPowerSlotsPayload.TYPE, RerollPowerSlotsPayload.STREAM_CODEC);
		PayloadTypeRegistry.clientboundPlay().register(PowerStatePayload.TYPE, PowerStatePayload.STREAM_CODEC);

		ServerPlayNetworking.registerGlobalReceiver(ActivateAbilityPayload.TYPE, PowersPackets::handleActivate);
		ServerPlayNetworking.registerGlobalReceiver(TeleportRequestPayload.TYPE, PowersPackets::handleTeleport);
		ServerPlayNetworking.registerGlobalReceiver(TeleportMarkPayload.TYPE, PowersPackets::handleMark);
		ServerPlayNetworking.registerGlobalReceiver(SetPowerSlotsPayload.TYPE, PowersPackets::handleSetSlots);
		ServerPlayNetworking.registerGlobalReceiver(RerollPowerSlotsPayload.TYPE, PowersPackets::handleReroll);
	}

	private static void handleActivate(ActivateAbilityPayload payload, ServerPlayNetworking.Context context) {
		context.server().execute(() -> {
			ServerPlayer player = context.player();
			AmethystDampening.update(player);
			if (AmethystDampening.isDampened(player)) {
				player.sendSystemMessage(Component.translatable("amethyst.powers.suppressed"));
				return;
			}
			if (SpaceTimeAbility.isFrozen(player)) return;
			if (SpaceTimeAbility.isFrozen(player)) return;
			if (payload.slot() < 0 || payload.slot() >= PlayerPowers.SLOT_COUNT) return;
			PlayerPowers.PlayerPowersData data = PlayerPowers.get(player);
			Power power = data.getPower(payload.slot());
			if (power == null) return;
			Ability ability = power.ability();
			if (ability == null || ability.requiresInput()) return;
			String powerId = power.id().toString();

			if (ability.isToggle()) {
				if (data.isToggleActive(powerId)) {
					ability.activateToggleOff(player, data);
					data.setToggleActive(player, powerId, false);
				} else {
					boolean paid = data.spendEnergy(player, ability);
					if (paid && ability.activateToggleOn(player, data)) {
						data.setToggleActive(player, powerId, true);
					} else if (paid) {
						data.refundEnergy(PowerEnergy.cost(ability));
					}
				}
				return;
			}

			if (!data.spendEnergy(player, ability)) return;
			if (!ability.activate(player, data)) {
				data.refundEnergy(PowerEnergy.cost(ability));
			}
			syncTo(player);
		});
	}

	private static void handleTeleport(TeleportRequestPayload payload, ServerPlayNetworking.Context context) {
		context.server().execute(() -> {
			ServerPlayer player = context.player();
			AmethystDampening.update(player);
			PlayerPowers.PlayerPowersData data = PlayerPowers.get(player);
			if (AmethystDampening.isDampened(player)) {
				player.sendSystemMessage(Component.translatable("amethyst.powers.suppressed"));
				return;
			}
			if (payload.slot() < 0 || payload.slot() >= PlayerPowers.SLOT_COUNT
					|| payload.targetName().length() > 16
					|| !Double.isFinite(payload.x()) || !Double.isFinite(payload.y()) || !Double.isFinite(payload.z())) return;
			Power power = data.getPower(payload.slot());
			if (power == null || power.ability() == null || !power.ability().requiresInput()) return;

			if (payload.toPlayer()) {
				ServerPlayer target = findPlayer(player, payload.targetName());
				if (target == null) return;
				if (!data.spendEnergy(player, power.ability())) return;
				TeleportAbility.startMarking(player, target, payload.slot());
				syncTo(player);
				return;
			}

			ServerPlayer subject = payload.targetName().isEmpty()
					? player : findPlayer(player, payload.targetName());
			if (subject == null) return;
			if (AmethystDampening.isDampened(subject)) {
				player.sendSystemMessage(Component.translatable("amethyst.powers.target_protected"));
				return;
			}

			Ability ability = power.ability();
			if (!data.spendEnergy(player, ability)) return;
			if (!ability.activateTeleport(subject, data, payload.dimension(), payload.x(), payload.y(), payload.z())) {
				data.refundEnergy(PowerEnergy.cost(ability));
			}
			syncTo(player);
		});
	}

	private static void handleMark(TeleportMarkPayload payload, ServerPlayNetworking.Context context) {
		context.server().execute(() -> {
			ServerPlayer player = context.player();
			if (payload.slot() < 0 || payload.slot() >= PlayerPowers.SLOT_COUNT) return;
			TeleportAbility.completeMarking(player, payload.slot(), player.position());
		});
	}

	private static ServerPlayer findPlayer(ServerPlayer caster, String name) {
		for (ServerPlayer p : ((net.minecraft.server.level.ServerLevel) caster.level()).getServer().getPlayerList().getPlayers()) {
			if (p.getName().getString().equalsIgnoreCase(name)) {
				return p;
			}
		}
		caster.sendSystemMessage(net.minecraft.network.chat.Component.literal("Player not found: " + name));
		return null;
	}

	private static void handleSetSlots(SetPowerSlotsPayload payload, ServerPlayNetworking.Context context) {
		context.server().execute(() -> {
			ServerPlayer player = context.player();
			if (!PlayerPowers.PlayerPowersData.validateSlots(payload.powerIds())) return;
			PlayerPowers.get(player).setSlots(player, payload.powerIds());
		});
	}

	private static void handleReroll(RerollPowerSlotsPayload payload, ServerPlayNetworking.Context context) {
		context.server().execute(() ->
				PlayerPowers.get(context.player()).assignRandom(context.player(), true));
	}

	public static void syncTo(ServerPlayer player) {
		PlayerPowers.PlayerPowersData data = PlayerPowers.get(player);
		ServerPlayNetworking.send(player, new PowerStatePayload(
				data.getSlotIds(),
				data.getActiveToggles(),
				data.energy()));
	}
}
