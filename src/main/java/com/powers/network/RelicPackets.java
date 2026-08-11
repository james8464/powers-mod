package com.powers.network;

import com.powers.PowersMod;
import com.powers.item.ArtifactEnergyReservoir;
import com.powers.item.ArtifactRole;
import com.powers.item.ImportedArtifactItem;
import com.powers.player.PlayerPowers;
import com.powers.power.Power;
import com.powers.power.PowerEnergy;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Server-authored relic menus and direction-only reservoir transfer requests. */
public final class RelicPackets {
	public record OpenReservoirPayload(int slot, int mainEnergy, int mainCapacity,
			int auxiliaryEnergy, int auxiliaryCapacity, int pendingCost,
			int pendingShortfall) implements CustomPacketPayload {
		public static final Type<OpenReservoirPayload> TYPE =
				new Type<>(PowersMod.id("open_reservoir"));
		public static final StreamCodec<RegistryFriendlyByteBuf, OpenReservoirPayload> STREAM_CODEC =
				StreamCodec.of((buffer, payload) -> {
					buffer.writeVarInt(payload.slot());
					buffer.writeVarInt(payload.mainEnergy());
					buffer.writeVarInt(payload.mainCapacity());
					buffer.writeVarInt(payload.auxiliaryEnergy());
					buffer.writeVarInt(payload.auxiliaryCapacity());
					buffer.writeVarInt(payload.pendingCost());
					buffer.writeVarInt(payload.pendingShortfall());
				}, buffer -> new OpenReservoirPayload(buffer.readVarInt(), buffer.readVarInt(),
						buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(),
						buffer.readVarInt(), buffer.readVarInt()));

		@Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
	}

	public record TransferReservoirPayload(int slot, boolean release)
			implements CustomPacketPayload {
		public static final Type<TransferReservoirPayload> TYPE =
				new Type<>(PowersMod.id("transfer_reservoir"));
		public static final StreamCodec<RegistryFriendlyByteBuf, TransferReservoirPayload> STREAM_CODEC =
				StreamCodec.composite(ByteBufCodecs.VAR_INT, TransferReservoirPayload::slot,
						ByteBufCodecs.BOOL, TransferReservoirPayload::release,
						TransferReservoirPayload::new);

		@Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
	}

	private RelicPackets() {
	}

	static void initialize() {
		PayloadTypeRegistry.clientboundPlay().register(
				OpenReservoirPayload.TYPE, OpenReservoirPayload.STREAM_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(
				TransferReservoirPayload.TYPE, TransferReservoirPayload.STREAM_CODEC);
		ServerPlayNetworking.registerGlobalReceiver(TransferReservoirPayload.TYPE,
				(payload, context) -> context.server().execute(() -> {
					if (PacketRateLimiter.allow(context.player(), PacketRateLimiter.Lane.ARTIFACT)) {
						transfer(context.player(), payload);
					}
				}));
	}

	public static boolean openReservoir(ServerPlayer player, ItemStack expected) {
		int slot = exactSlot(player, expected);
		if (slot < 0 || !isReservoir(expected)) return false;
		sendState(player, slot, expected);
		return true;
	}

	private static void transfer(ServerPlayer player, TransferReservoirPayload payload) {
		if (payload.slot() < 0 || payload.slot() >= player.getInventory().getContainerSize()) return;
		ItemStack stack = player.getInventory().getItem(payload.slot());
		if (!isReservoir(stack)) return;
		ImportedArtifactItem relic = (ImportedArtifactItem) stack.getItem();
		PlayerPowers.PlayerPowersData data = PlayerPowers.get(player);
		ArtifactEnergyReservoir.Transfer result = ArtifactEnergyReservoir.transferStep(
				data.energy(), data.energyCapacity(), ArtifactEnergyReservoir.stored(stack),
				ArtifactEnergyReservoir.capacity(relic.texture()), payload.release()
						? ArtifactEnergyReservoir.Direction.RELEASE
						: ArtifactEnergyReservoir.Direction.STORE);
		if (!result.transferred()) {
			sendState(player, payload.slot(), stack);
			return;
		}
		if (result.mainEnergy() < data.energy()) data.drainEnergy(data.energy() - result.mainEnergy());
		else data.refundEnergy(result.mainEnergy() - data.energy());
		ArtifactEnergyReservoir.setStored(stack, result.auxiliaryEnergy());
		PowersPackets.syncTo(player);
		sendState(player, payload.slot(), stack);
	}

	private static void sendState(ServerPlayer player, int slot, ItemStack stack) {
		ImportedArtifactItem relic = (ImportedArtifactItem) stack.getItem();
		PlayerPowers.PlayerPowersData data = PlayerPowers.get(player);
		Power pending = data.getPower(0);
		int pendingCost = pending == null || pending.ability() == null
				? 0 : PowerEnergy.cost(player, pending.ability());
		int auxiliary = ArtifactEnergyReservoir.stored(stack);
		ServerPlayNetworking.send(player, new OpenReservoirPayload(slot, data.energy(),
				data.energyCapacity(), auxiliary, ArtifactEnergyReservoir.capacity(relic.texture()),
				pendingCost, ArtifactEnergyReservoir.shortfall(data.energy(), auxiliary, pendingCost)));
	}

	private static boolean isReservoir(ItemStack stack) {
		return stack.getItem() instanceof ImportedArtifactItem relic
				&& relic.role() == ArtifactRole.ENERGY_RESERVOIR;
	}

	private static int exactSlot(ServerPlayer player, ItemStack expected) {
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			if (player.getInventory().getItem(slot) == expected) return slot;
		}
		return -1;
	}
}
