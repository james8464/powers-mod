package com.powers.client;

import com.powers.PowersParticles;
import com.powers.network.CompanionPackets;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Owns collisionless client-local Shadow avatars for every authorised owner. */
public final class PrivateCompanionClient {
	private static final Map<UUID, Apparition> APPARITIONS = new HashMap<>();
	private static int nextLocalEntityId = -1_930_062_001;

	private static final class Apparition {
		private final long sessionId;
		private final ShadowRemotePlayer avatar;
		private Vec3 target;
		private float targetYaw;

		private Apparition(long sessionId, ShadowRemotePlayer avatar,
				Vec3 target, float targetYaw) {
			this.sessionId = sessionId;
			this.avatar = avatar;
			this.target = target;
			this.targetYaw = targetYaw;
		}
	}

	private PrivateCompanionClient() {
	}

	public static void handle(CompanionPackets.StatePayload payload) {
		Minecraft client = Minecraft.getInstance();
		if (!payload.active() || client.level == null || client.player == null
				|| !payload.dimension().equals(client.level.dimension().identifier().toString())) {
			remove(payload.ownerId());
			return;
		}
		Apparition current = APPARITIONS.get(payload.ownerId());
		if (current == null || current.sessionId != payload.sessionId()
				|| current.avatar.isRemoved() || current.avatar.level() != client.level) {
			remove(payload.ownerId());
			ShadowRemotePlayer avatar = new ShadowRemotePlayer(client.level, payload.ownerId());
			avatar.setId(nextLocalEntityId--);
			avatar.setPos(payload.x(), payload.y(), payload.z());
			avatar.setYRot(payload.yaw());
			avatar.setYHeadRot(payload.yaw());
			client.level.addEntity(avatar);
			current = new Apparition(payload.sessionId(), avatar,
					new Vec3(payload.x(), payload.y(), payload.z()), payload.yaw());
			APPARITIONS.put(payload.ownerId(), current);
			manifest(client, current.target);
		}
		current.target = new Vec3(payload.x(), payload.y(), payload.z());
		current.targetYaw = payload.yaw();
		if (payload.teleport()) {
			current.avatar.setPos(current.target);
			manifest(client, current.target);
		}
	}

	public static void tick() {
		Minecraft client = Minecraft.getInstance();
		for (UUID owner : java.util.List.copyOf(APPARITIONS.keySet())) {
			Apparition apparition = APPARITIONS.get(owner);
			if (apparition == null || apparition.avatar.isRemoved()
					|| client.level == null || apparition.avatar.level() != client.level) {
				remove(owner);
				continue;
			}
			Vec3 previous = apparition.avatar.position();
			Vec3 next = previous.lerp(apparition.target, 0.34);
			apparition.avatar.setDeltaMovement(next.subtract(previous));
			apparition.avatar.setPos(next);
			apparition.avatar.setYRot(rotateToward(apparition.avatar.getYRot(),
					apparition.targetYaw, 0.28F));
			apparition.avatar.setYHeadRot(apparition.avatar.getYRot());
		}
	}

	/** G toggles the owner's own Shadow; questions and visibility use chat. */
	public static void interact() {
		Minecraft client = Minecraft.getInstance();
		if (client.player == null) return;
		Apparition own = APPARITIONS.get(client.player.getUUID());
		long request = own == null || own.avatar.isRemoved() ? -1L : -2L;
		net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(
				new CompanionPackets.InteractPayload(request));
	}

	public static void clear() {
		for (UUID owner : java.util.List.copyOf(APPARITIONS.keySet())) remove(owner);
		APPARITIONS.clear();
	}

	private static void remove(UUID owner) {
		Apparition removed = APPARITIONS.remove(owner);
		Minecraft client = Minecraft.getInstance();
		if (removed != null && client.level != null && !removed.avatar.isRemoved()) {
			client.level.removeEntity(removed.avatar.getId(), Entity.RemovalReason.DISCARDED);
		}
	}

	private static void manifest(Minecraft client, Vec3 position) {
		if (client.level == null) return;
		for (int i = 0; i < 18; i++) {
			double angle = Math.PI * 2.0 * i / 18.0;
			double y = position.y + 0.1 + i % 6 * 0.32;
			client.level.addParticle(PowersParticles.ECLIPSE,
					position.x + Math.cos(angle) * 0.48, y,
					position.z + Math.sin(angle) * 0.48,
					0.0, 0.015, 0.0);
		}
	}

	private static float rotateToward(float from, float to, float fraction) {
		float difference = net.minecraft.util.Mth.wrapDegrees(to - from);
		return from + difference * fraction;
	}
}
