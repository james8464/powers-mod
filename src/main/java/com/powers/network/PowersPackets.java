package com.powers.network;

import com.powers.PowersMod;
import com.powers.player.PlayerPowers;
import com.powers.power.Ability;
import com.powers.power.Power;
import com.powers.power.ActivationCooldowns;
import com.powers.power.AmethystDampening;
import com.powers.power.crystals.SpaceTimeAbility;
import com.powers.power.abilities.TeleportAbility;
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
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public final class PowersPackets {
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

	public record PowerStatePayload(List<String> powerIds, List<String> activeToggles, int energy,
			int energyCapacity) implements CustomPacketPayload {
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
						ByteBufCodecs.VAR_INT,
						PowerStatePayload::energyCapacity,
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
		PayloadTypeRegistry.clientboundPlay().register(PowerStatePayload.TYPE, PowerStatePayload.STREAM_CODEC);

		ServerPlayNetworking.registerGlobalReceiver(ActivateAbilityPayload.TYPE, PowersPackets::handleActivate);
		ServerPlayNetworking.registerGlobalReceiver(TeleportRequestPayload.TYPE, PowersPackets::handleTeleport);
		ServerPlayNetworking.registerGlobalReceiver(TeleportMarkPayload.TYPE, PowersPackets::handleMark);
	}

	private static void handleActivate(ActivateAbilityPayload payload, ServerPlayNetworking.Context context) {
		context.server().execute(() -> {
			ServerPlayer player = context.player();
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
						data.refundEnergy(ability);
					}
				}
				return;
			}

			if (!ActivationCooldowns.isReady(player, ability)) {
				PowerMessages.send(player, "ability.powers.cooldown", 4,
						seconds(ActivationCooldowns.remainingTicks(player, ability)));
				return;
			}
			if (!data.spendEnergy(player, ability)) return;
			if (!ability.activate(player, data)) {
				data.refundEnergy(ability);
			} else {
				ActivationCooldowns.start(player, ability, ability.cooldownTicksFor(player, data));
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
				AmethystDampening.punish(player);
				return;
			}
			if (SpaceTimeAbility.isFrozen(player)) {
				SpaceTimeAbility.reject(player);
				return;
			}
			if (payload.slot() < 0 || payload.slot() >= PlayerPowers.SLOT_COUNT
					|| payload.targetName().length() > 16
					|| !Double.isFinite(payload.x()) || !Double.isFinite(payload.y()) || !Double.isFinite(payload.z())) return;
			Power power = data.getPower(payload.slot());
			if (power == null || power.ability() == null || !power.ability().requiresInput()) return;
			Ability ability = power.ability();

			if (payload.toPlayer()) {
				ServerPlayer target = findPlayer(player, payload.targetName());
				if (target == null) return;
				if (!ActivationCooldowns.isReady(player, ability)) {
					PowerMessages.send(player, "ability.powers.cooldown", 4,
							seconds(ActivationCooldowns.remainingTicks(player, ability)));
					return;
				}
				if (!data.spendEnergy(player, ability)) return;
				TeleportAbility.startMarking(player, target, payload.slot());
				ActivationCooldowns.start(player, ability, ability.cooldownTicksFor(player, data));
				syncTo(player);
				return;
			}

			ServerPlayer subject = payload.targetName().isEmpty()
					? player : findPlayer(player, payload.targetName());
			if (subject == null) return;
			if (AmethystDampening.isDampened(subject)) {
				PowerMessages.send(player, "amethyst.powers.target_protected", 4);
				return;
			}
			if (!ActivationCooldowns.isReady(player, ability)) {
				PowerMessages.send(player, "ability.powers.cooldown", 4,
						seconds(ActivationCooldowns.remainingTicks(player, ability)));
				return;
			}
			if (!data.spendEnergy(player, ability)) return;
			if (!ability.activateTeleport(player, subject, data, payload.dimension(), payload.x(), payload.y(), payload.z())) {
				data.refundEnergy(ability);
			} else {
				ActivationCooldowns.start(player, ability, ability.cooldownTicksFor(player, data));
			}
			syncTo(player);
		});
	}

	private static void handleMark(TeleportMarkPayload payload, ServerPlayNetworking.Context context) {
		context.server().execute(() -> {
			ServerPlayer player = context.player();
			if (payload.slot() < 0 || payload.slot() >= PlayerPowers.SLOT_COUNT) return;
			if (!Double.isFinite(payload.x()) || !Double.isFinite(payload.y()) || !Double.isFinite(payload.z())) return;
			AmethystDampening.update(player);
			if (AmethystDampening.isDampened(player)) {
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

	private static String seconds(int ticks) {
		return String.valueOf((ticks + 19) / 20);
	}

	public static void syncTo(ServerPlayer player) {
		PlayerPowers.PlayerPowersData data = PlayerPowers.get(player);
		ServerPlayNetworking.send(player, new PowerStatePayload(
				data.getSlotIds(),
				data.getActiveToggles(),
				data.energy(),
				data.energyCapacity()));
	}
}
