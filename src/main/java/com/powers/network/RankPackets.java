package com.powers.network;

import com.powers.PowersMod;
import com.powers.PowersSounds;
import com.powers.fx.PowerFx;
import com.powers.player.PlayerPowers;
import com.powers.player.SkillSystem;
import com.powers.progression.RankGraph;
import com.powers.progression.RankGraphRegistry;
import com.powers.progression.RankNode;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.regex.Pattern;

/** Owns request-only rank-maze actions and their server-side eligibility checks. */
public final class RankPackets {
	private static final Pattern NODE_ID = Pattern.compile("[a-z0-9_]{1,48}");
	private static final StreamCodec<io.netty.buffer.ByteBuf, String> NODE_ID_CODEC =
			ByteBufCodecs.stringUtf8(48);

	private RankPackets() {
	}

	/** Request-only rank action; the server derives graph, depth, and eligibility. */
	public record RankActionPayload(String nodeId, boolean focus) implements CustomPacketPayload {
		public static final CustomPacketPayload.Type<RankActionPayload> TYPE =
				new CustomPacketPayload.Type<>(PowersMod.id("rank_action"));
		public static final StreamCodec<RegistryFriendlyByteBuf, RankActionPayload> STREAM_CODEC =
				StreamCodec.composite(
						NODE_ID_CODEC, RankActionPayload::nodeId,
						ByteBufCodecs.BOOL, RankActionPayload::focus,
						RankActionPayload::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public static void initialize() {
		PayloadTypeRegistry.serverboundPlay().register(RankActionPayload.TYPE, RankActionPayload.STREAM_CODEC);
		PowersPlayNetworking.registerReceiver(RankActionPayload.TYPE, RankPackets::handle);
	}

	private static void handle(RankActionPayload payload, ServerPlayer player) {
		apply(player, payload);
	}

	private static void apply(ServerPlayer player, RankActionPayload payload) {
		if (!PacketRateLimiter.allow(player, PacketRateLimiter.Lane.RANK)) return;
		if (!NODE_ID.matcher(payload.nodeId()).matches()) return;
		boolean darkness = SkillSystem.hasDarknessTag(player);
		PlayerPowers.PlayerPowersData data = PlayerPowers.get(player);
		RankGraph graph = darkness ? RankGraphRegistry.darkness() : RankGraphRegistry.light();
		RankNode node = graph.node(payload.nodeId());
		if (node == null) return;
		boolean changed = payload.focus()
				? data.setRankFocus(darkness, payload.nodeId())
				: data.unlockRankNode(darkness, payload.nodeId());
		if (!changed) return;
		SkillSystem.refreshPrefix(player);
		ServerLevel level = (ServerLevel) player.level();
		Vec3 center = player.position().add(0, 1.0, 0);
		PowerFx.rune(level, center, 1.25, darkness ? 0x7C36C8 : 0xFFE08A, 26,
				Math.floorMod(node.id().hashCode(), 360) * Math.PI / 180.0);
		PowerFx.spiral(level, center.add(0, -0.5, 0), 0.8, 1.8,
				darkness ? 0xA456E8 : 0xFFF1B8, 32, 0.0);
		PowerFx.sound(level, center, PowersSounds.RANK_AWAKEN, 1.0f, payload.focus() ? 1.12f : 0.92f);
		PowersPackets.syncTo(player);
	}
}
