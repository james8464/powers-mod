package com.powers.network;

import com.powers.util.ServerCallbackGate;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

/** Re-resolves queued play packets against the same connection body and server lifecycle epoch. */
final class ServerPlayCallback {
	private ServerPlayCallback() {
	}

	static void execute(ServerPlayNetworking.Context context, Consumer<ServerPlayer> action) {
		Objects.requireNonNull(context, "context");
		Objects.requireNonNull(action, "action");
		ServerPlayer received = context.player();
		UUID playerId = received.getUUID();
		int entityId = received.getId();
		ResourceKey<Level> dimension = received.level().dimension();
		long epoch = ServerCallbackGate.capture(context.server());
		ServerCallbackGate.execute(epoch, server -> {
			var level = server.getLevel(dimension);
			ServerPlayer current = level != null && level.getEntity(entityId) instanceof ServerPlayer found
					? found : null;
			if (current != null && current.getId() == entityId && current.connection != null
					&& current.getUUID().equals(playerId) && !current.isRemoved()
					&& current.level().dimension().equals(dimension)) {
				action.accept(current);
			}
		});
	}
}
