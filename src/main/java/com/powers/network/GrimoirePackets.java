package com.powers.network;

import com.powers.PowersMod;
import com.powers.player.PlayerPowers;
import com.powers.spell.GrimoireDefinition;
import com.powers.spell.SpellCastingManager;
import com.powers.spell.SpellIndexEntry;
import com.powers.util.PowerMessages;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/** Server-authoritative grimoire contents snapshots and bounded page selection. */
public final class GrimoirePackets {
	private static final int MAX_BOOK_KEY = 64;
	private static final int MAX_SPELL_ID = 64;
	private static final int MAX_ENTRIES = 16;

	/** Complete compact book snapshot; only canonical registry data crosses the network. */
	public record OpenIndexPayload(String grimoireKey, int selected,
			List<SpellIndexEntry> entries) implements CustomPacketPayload {
		public static final Type<OpenIndexPayload> TYPE = new Type<>(PowersMod.id("open_grimoire_index"));
		public static final StreamCodec<RegistryFriendlyByteBuf, OpenIndexPayload> STREAM_CODEC =
				StreamCodec.of(GrimoirePackets::encodeIndex, GrimoirePackets::decodeIndex);

		public OpenIndexPayload {
			entries = List.copyOf(entries);
			if (entries.isEmpty() || entries.size() > MAX_ENTRIES) {
				throw new IllegalArgumentException("Invalid grimoire index size");
			}
		}

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** Untrusted row selection; the server re-resolves the held canonical book. */
	public record SelectSpellPayload(String grimoireKey, int selected) implements CustomPacketPayload {
		public static final Type<SelectSpellPayload> TYPE = new Type<>(PowersMod.id("select_grimoire_spell"));
		public static final StreamCodec<RegistryFriendlyByteBuf, SelectSpellPayload> STREAM_CODEC =
				StreamCodec.of((buffer, payload) -> {
					buffer.writeUtf(payload.grimoireKey(), MAX_BOOK_KEY);
					buffer.writeVarInt(payload.selected());
				}, buffer -> new SelectSpellPayload(buffer.readUtf(MAX_BOOK_KEY), buffer.readVarInt()));

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	private GrimoirePackets() {
	}

	/** Registers both directions and the validated selection handler. */
	public static void initialize() {
		PayloadTypeRegistry.clientboundPlay().register(OpenIndexPayload.TYPE, OpenIndexPayload.STREAM_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(SelectSpellPayload.TYPE, SelectSpellPayload.STREAM_CODEC);
		ServerPlayNetworking.registerGlobalReceiver(SelectSpellPayload.TYPE,
				(payload, context) -> ServerPlayCallback.execute(context, player -> select(player, payload)));
	}

	/** Opens a canonical snapshot for one held grimoire. */
	public static void open(ServerPlayer player, GrimoireDefinition grimoire) {
		List<SpellIndexEntry> entries = grimoire.spells().stream().map(SpellIndexEntry::from).toList();
		int selected = PlayerPowers.get(player).selectedSpell(grimoire.key(), entries.size());
		ServerPlayNetworking.send(player, new OpenIndexPayload(grimoire.key(), selected, entries));
	}

	private static void select(ServerPlayer player, SelectSpellPayload payload) {
		if (!PacketRateLimiter.allow(player, PacketRateLimiter.Lane.SELECTION)) return;
		if (payload.selected() < 0 || payload.selected() >= MAX_ENTRIES) return;
		GrimoireDefinition held = SpellCastingManager.heldDefinition(player);
		if (held == null || !held.key().equals(payload.grimoireKey())
				|| payload.selected() >= held.spells().size()) return;
		PlayerPowers.get(player).setSelectedSpell(held.key(), payload.selected());
		PowerMessages.overlay(player, Component.translatable("spell.powers.selected",
				Component.translatable("spell.powers." + held.spells().get(payload.selected()).id())));
	}

	private static void encodeIndex(RegistryFriendlyByteBuf buffer, OpenIndexPayload payload) {
		buffer.writeUtf(payload.grimoireKey(), MAX_BOOK_KEY);
		buffer.writeVarInt(payload.selected());
		buffer.writeVarInt(payload.entries().size());
		for (SpellIndexEntry entry : payload.entries()) {
			buffer.writeUtf(entry.id(), MAX_SPELL_ID);
			buffer.writeVarInt(entry.energy());
			buffer.writeVarInt(entry.cooldownTicks());
			buffer.writeVarInt(entry.channelTicks());
			buffer.writeDouble(entry.range());
		}
	}

	private static OpenIndexPayload decodeIndex(RegistryFriendlyByteBuf buffer) {
		String key = buffer.readUtf(MAX_BOOK_KEY);
		int selected = buffer.readVarInt();
		int count = buffer.readVarInt();
		if (count <= 0 || count > MAX_ENTRIES) throw new IllegalArgumentException("Invalid grimoire entry count");
		List<SpellIndexEntry> entries = new ArrayList<>(count);
		for (int index = 0; index < count; index++) {
			String id = buffer.readUtf(MAX_SPELL_ID);
			int energy = buffer.readVarInt();
			int cooldown = buffer.readVarInt();
			int channel = buffer.readVarInt();
			double range = buffer.readDouble();
			String base = "spell.powers.index.";
			entries.add(new SpellIndexEntry(id, energy, cooldown, channel, range,
					base + "purpose." + id, base + "target." + id, base + "counter." + id));
		}
		return new OpenIndexPayload(key, selected, entries);
	}
}
