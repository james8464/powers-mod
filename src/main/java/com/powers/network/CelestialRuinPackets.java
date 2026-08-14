package com.powers.network;

import com.powers.PowersMod;
import com.powers.diagnostics.ServerRuntimeMetrics;
import com.powers.fx.FxLodPolicy;
import com.powers.fx.FxLodScope;
import com.powers.fx.FxLodTier;
import com.powers.fx.FxShapeFamily;
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

	public record Payload(Phase phase, double x, double y, double z, int age, FxLodTier lod)
			implements CustomPacketPayload {
		public static final Type<Payload> TYPE = new Type<>(PowersMod.id("celestial_ruin_fx"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Payload> STREAM_CODEC =
				StreamCodec.of(Payload::encode, Payload::decode);

		public Payload {
			java.util.Objects.requireNonNull(phase, "phase");
			java.util.Objects.requireNonNull(lod, "lod");
			if (lod == FxLodTier.HIDDEN) {
				throw new IllegalArgumentException("Hidden Celestial Ruin cues must not be sent");
			}
			if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
				throw new IllegalArgumentException("Celestial Ruin position must be finite");
			}
			age = Math.max(0, age);
		}

		private static void encode(RegistryFriendlyByteBuf buffer, Payload payload) {
			buffer.writeByte(payload.phase.ordinal());
			buffer.writeDouble(payload.x);
			buffer.writeDouble(payload.y);
			buffer.writeDouble(payload.z);
			buffer.writeVarInt(Math.max(0, payload.age));
			buffer.writeByte(payload.lod.networkId());
		}

		private static Payload decode(RegistryFriendlyByteBuf buffer) {
			int phase = Math.clamp(buffer.readUnsignedByte(), 0, Phase.values().length - 1);
			return new Payload(Phase.values()[phase], buffer.readDouble(), buffer.readDouble(),
					buffer.readDouble(), buffer.readVarInt(),
					FxLodTier.fromNetworkId(buffer.readUnsignedByte()));
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
		for (ServerPlayer player : level.players()) {
			double distanceSquared = player.position().distanceToSqr(center);
			if (distanceSquared > range * range || !ServerPlayNetworking.canSend(player, Payload.TYPE)) continue;
			var lod = FxLodPolicy.decide(Math.sqrt(distanceSquared), 640,
					FxLodScope.CATASTROPHIC, FxShapeFamily.COLUMN);
			if (!lod.visible()) continue;
			ServerPlayNetworking.send(player,
					new Payload(phase, center.x, center.y, center.z, age, lod.tier()));
			ServerRuntimeMetrics.recordPacket(level.getServer(), level.getServer().getTickCount());
		}
	}
}
