package com.powers.power.abilities;

import com.powers.magic.runtime.CastSource;
import com.powers.power.AsyncAbilityTransaction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Stable identity-only continuations for Time Shift's two delayed travel routes. */
final class TeleportDelayedState {
	static final Map<UUID, PendingMarking> PENDING_MARKING = new HashMap<>();
	static final Map<UUID, PendingTeleport> PENDING_TELEPORTS = new HashMap<>();
	static final Map<UUID, MarkingState> MARKING = new HashMap<>();

	record MarkingState(ResourceKey<Level> originalDimension,
			Vec3 originalPos, GameType originalMode, ResourceKey<Level> markingDimension,
			Vec3 markingCenter, CastSource castSource, long deadline, int slot) { }

	record PendingMarking(MarkingState state,
			ResourceKey<Level> destination, Vec3 position) { }

	record PendingTeleport(UUID casterId, UUID travellerId,
			ResourceKey<Level> origin, ResourceKey<Level> destination, Vec3 target,
			CastSource castSource, int stormTicks, int teleportDelay,
			AsyncAbilityTransaction transaction, boolean bodyStarted) {
		PendingTeleport withBodyStarted(boolean started) {
			return new PendingTeleport(casterId, travellerId, origin, destination, target,
					castSource, stormTicks, teleportDelay, transaction, started);
		}
	}

	private TeleportDelayedState() {
	}

	static LivingEntity findLiving(MinecraftServer server, UUID entityId) {
		ServerPlayer player = server.getPlayerList().getPlayer(entityId);
		if (player != null) return player;
		for (ServerLevel level : server.getAllLevels()) {
			if (level.getEntity(entityId) instanceof LivingEntity living) return living;
		}
		return null;
	}
}
