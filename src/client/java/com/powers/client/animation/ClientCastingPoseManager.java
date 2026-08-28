package com.powers.client.animation;

import com.powers.animation.CastingPoseService;
import com.powers.animation.ClientCastingPoseState;
import com.powers.network.CastingPosePackets;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

import java.util.Optional;

/** Owns connection/world-epoch casting poses and resolves only exact live entity identities. */
public final class ClientCastingPoseManager {
	private static final ClientCastingPoseState state = new ClientCastingPoseState();
	private static long connectionEpoch = 1;
	private static long worldEpoch;
	private static Object observedWorld;

	private ClientCastingPoseManager() {
	}

	public static synchronized ClientCastingPoseState.HandlerStamp captureHandlerStamp(
			Minecraft client) {
		observeWorld(client);
		return new ClientCastingPoseState.HandlerStamp(connectionEpoch, worldEpoch);
	}

	public static synchronized void handle(CastingPosePackets.Payload payload,
			ClientCastingPoseState.HandlerStamp captured) {
		Minecraft client = Minecraft.getInstance();
		observeWorld(client);
		if (client.level == null) return;
		Entity entity = client.level.getEntity(payload.entityId());
		if (entity == null) return;
		ClientCastingPoseState.WorldIdentity world = worldIdentity();
		ClientCastingPoseState.EntityIdentity identity = new ClientCastingPoseState.EntityIdentity(
				entity.getId(), entity.getUUID(), CastingPoseService.scopeType(entity.getClass()));
		var event = payload.event();
		state.accept(new ClientCastingPoseState.Wire(event.entityId(), event.entityUuid(),
				event.sequence(), event.pose(), event.style(), event.hand(), event.startGameTime(),
				event.durationTicks(), event.terminal()), captured, world, identity,
				client.level.getGameTime());
	}

	public static synchronized Optional<ClientCastingPoseState.Resolved> resolve(Entity entity) {
		Minecraft client = Minecraft.getInstance();
		observeWorld(client);
		if (client.level == null || entity.level() != client.level
				|| !CastingPoseService.scopeType(entity.getClass())) return Optional.empty();
		return state.resolve(entity.getUUID(), client.level.getGameTime());
	}

	public static synchronized void tick(Minecraft client) {
		observeWorld(client);
		if (client.level != null) state.tick(client.level.getGameTime());
	}

	public static synchronized void resetConnectionEpoch() {
		connectionEpoch = connectionEpoch == Long.MAX_VALUE ? 1 : connectionEpoch + 1;
		worldEpoch = 0;
		observedWorld = null;
		state.reset(worldIdentity());
	}

	private static void observeWorld(Minecraft client) {
		if (client.level == observedWorld) return;
		observedWorld = client.level;
		worldEpoch = worldEpoch == Long.MAX_VALUE ? 1 : worldEpoch + 1;
		state.reset(worldIdentity());
	}

	private static ClientCastingPoseState.WorldIdentity worldIdentity() {
		return new ClientCastingPoseState.WorldIdentity(connectionEpoch, worldEpoch);
	}
}
