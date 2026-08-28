package com.powers.audio;

import com.powers.network.LayeredAudioPackets;
import com.powers.network.PowersPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Bounded server fan-out for one committed semantic magic sound. */
public final class LayeredAudioService {
	public static final int EVENT_SEQUENCE_BITS = 16;
	private static final int MAX_EVENT_SEQUENCE = (1 << EVENT_SEQUENCE_BITS) - 1;
	private static long sequenceTick = Long.MIN_VALUE;
	private static int sequence;

	private LayeredAudioService() {
	}

	/** Emits to same-dimension payload-capable listeners inside the cue's authored far radius. */
	public static int emit(ServerLevel level, Vec3 origin, LayeredAudioCue cue,
			float gain, float pitch) {
		Objects.requireNonNull(level, "level");
		return deliver(new MinecraftRuntime(level), origin, cue, gain, pitch);
	}

	/** Testable policy seam; each accepted observer receives exactly one immutable payload. */
	public static int deliver(RuntimeAccess runtime, Vec3 origin, LayeredAudioCue cue,
			float gain, float pitch) {
		Objects.requireNonNull(runtime, "runtime");
		Objects.requireNonNull(origin, "origin");
		Objects.requireNonNull(cue, "cue");
		long gameTime = runtime.gameTime();
		long eventId = nextEventId(gameTime);
		Identifier dimension = Objects.requireNonNull(runtime.dimension(), "dimension");
		float payloadGain = Float.isFinite(gain) ? Math.min(gain, 4.0F) : gain;
		LayeredAudioPackets.Payload payload = new LayeredAudioPackets.Payload(eventId, cue,
				dimension, origin.x, origin.y, origin.z, payloadGain, pitch, gameTime);
		double radius = cue.profile().maximumRadius();
		double radiusSquared = radius * radius;
		int sent = 0;
		for (Observer observer : runtime.observers()) {
			if (!dimension.equals(observer.dimension())
					|| observer.distanceSquared(origin) > radiusSquared
					|| !runtime.canSend(observer.player())) continue;
			runtime.send(observer.player(), payload);
			sent++;
		}
		return sent;
	}

	private static synchronized long nextEventId(long gameTime) {
		if (gameTime < 0 || gameTime > (Long.MAX_VALUE >>> EVENT_SEQUENCE_BITS)) {
			throw new IllegalArgumentException("Invalid layered audio game time");
		}
		if (gameTime != sequenceTick) {
			sequenceTick = gameTime;
			sequence = 0;
		} else if (sequence == MAX_EVENT_SEQUENCE) {
			throw new IllegalStateException("Layered audio event sequence exhausted for tick " + gameTime);
		} else {
			sequence++;
		}
		return (gameTime << EVENT_SEQUENCE_BITS) | sequence;
	}

	public record Observer(UUID player, Identifier dimension, double eyeX, double eyeY, double eyeZ) {
		public Observer {
			Objects.requireNonNull(player, "player");
			Objects.requireNonNull(dimension, "dimension");
			if (!Double.isFinite(eyeX) || !Double.isFinite(eyeY) || !Double.isFinite(eyeZ)) {
				throw new IllegalArgumentException("Observer eye position must be finite");
			}
		}

		private double distanceSquared(Vec3 origin) {
			double dx = eyeX - origin.x;
			double dy = eyeY - origin.y;
			double dz = eyeZ - origin.z;
			return dx * dx + dy * dy + dz * dz;
		}
	}

	public interface RuntimeAccess {
		long gameTime();
		Identifier dimension();
		List<Observer> observers();
		boolean canSend(UUID observer);
		void send(UUID observer, LayeredAudioPackets.Payload payload);
	}

	private static final class MinecraftRuntime implements RuntimeAccess {
		private final ServerLevel level;

		private MinecraftRuntime(ServerLevel level) {
			this.level = level;
		}

		@Override public long gameTime() { return level.getGameTime(); }
		@Override public Identifier dimension() { return level.dimension().identifier(); }

		@Override
		public List<Observer> observers() {
			return level.players().stream().map(player -> {
				Vec3 eye = player.getEyePosition();
				return new Observer(player.getUUID(), player.level().dimension().identifier(),
						eye.x, eye.y, eye.z);
			}).toList();
		}

		@Override
		public boolean canSend(UUID observer) {
			ServerPlayer player = level.getServer().getPlayerList().getPlayer(observer);
			return player != null && player.level() == level
					&& ServerPlayNetworking.canSend(player, LayeredAudioPackets.Payload.TYPE);
		}

		@Override
		public void send(UUID observer, LayeredAudioPackets.Payload payload) {
			ServerPlayer player = level.getServer().getPlayerList().getPlayer(observer);
			if (player != null && player.level() == level) PowersPlayNetworking.send(player, payload);
		}
	}
}
