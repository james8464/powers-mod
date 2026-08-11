package com.powers.network;

import com.powers.PowersMod;
import com.powers.diagnostics.ServerRuntimeMetrics;
import com.powers.spell.CelestialRuinRules;
import com.powers.spell.CelestialRuinPresentation;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/** Compact persistent-column and detonation cues; clients generate the large geometry locally. */
public final class CelestialRuinPackets {
	public enum Phase { BEGIN, SUSTAIN, DETONATE, END }

	public record Payload(Phase phase, double x, double y, double z, int age)
			implements CustomPacketPayload {
		public static final Type<Payload> TYPE = new Type<>(PowersMod.id("celestial_ruin_fx"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Payload> STREAM_CODEC =
				StreamCodec.of(Payload::encode, Payload::decode);

		private static void encode(RegistryFriendlyByteBuf buffer, Payload payload) {
			buffer.writeByte(payload.phase.ordinal());
			buffer.writeDouble(payload.x);
			buffer.writeDouble(payload.y);
			buffer.writeDouble(payload.z);
			buffer.writeVarInt(Math.max(0, payload.age));
		}

		private static Payload decode(RegistryFriendlyByteBuf buffer) {
			int phase = Math.clamp(buffer.readUnsignedByte(), 0, Phase.values().length - 1);
			return new Payload(Phase.values()[phase], buffer.readDouble(), buffer.readDouble(),
					buffer.readDouble(), buffer.readVarInt());
		}

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	private CelestialRuinPackets() {
	}

	public static void initialize() {
		PayloadTypeRegistry.clientboundPlay().register(Payload.TYPE, Payload.STREAM_CODEC);
	}

	public static void broadcast(ServerLevel level, Vec3 center, Phase phase, int age) {
		double range = phase == Phase.DETONATE
				? CelestialRuinRules.DAMAGE_RADIUS : CelestialRuinPresentation.BEAM_VIEW_RADIUS;
		Payload payload = new Payload(phase, center.x, center.y, center.z, age);
		for (ServerPlayer player : level.players()) {
			if (player.position().distanceToSqr(center) > range * range
					|| !ServerPlayNetworking.canSend(player, Payload.TYPE)) continue;
			ServerPlayNetworking.send(player, payload);
			ServerRuntimeMetrics.recordPacket(level.getServer(), level.getServer().getTickCount());
		}
	}
}
