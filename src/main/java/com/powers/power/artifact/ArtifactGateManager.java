package com.powers.power.artifact;

import com.powers.fx.PowerFx;
import com.powers.item.artifact.ArtifactAlignment;
import com.powers.item.ArtifactWeaponManager;
import com.powers.power.travel.SafeDestinationResolver;
import com.powers.power.travel.TravelKind;
import com.powers.util.LoadedChunks;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Owns one temporary, collision-safe paired gate per artifact wielder. */
public final class ArtifactGateManager {
	private static final int DURATION_TICKS = 20 * 30;
	private static final Map<UUID, Gate> GATES = new HashMap<>();
	private static final Map<UUID, Long> RECENT_TRAVEL = new HashMap<>();

	private record Gate(ResourceKey<Level> dimension, Vec3 entrance, Vec3 exit,
			ArtifactAlignment alignment, long expiresAt) {
	}

	private ArtifactGateManager() {
	}

	public static boolean open(ServerPlayer player, ArtifactAlignment alignment) {
		ServerLevel level = (ServerLevel) player.level();
		Vec3 entrance = player.position();
		Vec3 exit = findExit(player, level);
		if (exit == null || exit.distanceToSqr(entrance) < 16.0) return false;
		GATES.put(player.getUUID(), new Gate(level.dimension(), entrance, exit, alignment,
				level.getServer().getTickCount() + DURATION_TICKS));
		draw(level, entrance, alignment, 0.0);
		draw(level, exit, alignment, Math.PI);
		return true;
	}

	public static void tick(MinecraftServer server) {
		var iterator = GATES.entrySet().iterator();
		while (iterator.hasNext()) {
			var entry = iterator.next();
			Gate gate = entry.getValue();
			ServerLevel level = server.getLevel(gate.dimension());
			ServerPlayer owner = server.getPlayerList().getPlayer(entry.getKey());
			if (level == null || owner == null || !owner.isAlive()
					|| owner.level() != level
					|| !ArtifactWeaponManager.maySustain(owner, gate.alignment())
					|| server.getTickCount() >= gate.expiresAt()) {
				iterator.remove();
				RECENT_TRAVEL.remove(entry.getKey());
				continue;
			}
			if (server.getTickCount() % 10 == 0) {
				draw(level, gate.entrance(), gate.alignment(), server.getTickCount() * 0.08);
				draw(level, gate.exit(), gate.alignment(), -server.getTickCount() * 0.08);
			}
			long readyAt = RECENT_TRAVEL.getOrDefault(owner.getUUID(), 0L);
			if (server.getTickCount() < readyAt || owner.level() != level) continue;
			Vec3 destination = owner.distanceToSqr(gate.entrance()) <= 2.25 ? gate.exit()
					: owner.distanceToSqr(gate.exit()) <= 2.25 ? gate.entrance() : null;
			if (destination == null || !SafeDestinationResolver.validate(
					owner, level, destination, TravelKind.POWER).allowed()) continue;
			owner.teleport(new TeleportTransition(level, destination, Vec3.ZERO,
					owner.getYRot(), owner.getXRot(), TeleportTransition.PLAY_PORTAL_SOUND));
			RECENT_TRAVEL.put(owner.getUUID(), (long) server.getTickCount() + 20L);
		}
	}

	private static Vec3 findExit(ServerPlayer player, ServerLevel level) {
		Vec3 look = player.getLookAngle();
		for (int distance = 32; distance >= 4; distance--) {
			Vec3 candidate = player.position().add(look.scale(distance));
			BlockPos pos = BlockPos.containing(candidate);
			if (!LoadedChunks.contains(level, pos)) continue;
			for (int offset = 3; offset >= -3; offset--) {
				Vec3 adjusted = candidate.add(0.0, offset, 0.0);
				if (SafeDestinationResolver.validate(player, level, adjusted, TravelKind.POWER).allowed()) {
					return adjusted;
				}
			}
		}
		return null;
	}

	private static void draw(ServerLevel level, Vec3 center, ArtifactAlignment alignment, double phase) {
		int primary = alignment == ArtifactAlignment.DARKNESS ? 0x3A0B52 : 0xFFF2B2;
		int secondary = alignment == ArtifactAlignment.DARKNESS ? 0x7B3F91 : 0xFFFFFF;
		PowerFx.rune(level, center.add(0.0, 0.08, 0.0), 1.6, primary, 28, phase);
		PowerFx.ring(level, center.add(0.0, 1.0, 0.0), 1.0, secondary, 20, -phase);
	}

	public static void forget(UUID ownerId) {
		GATES.remove(ownerId);
		RECENT_TRAVEL.remove(ownerId);
	}

	public static void clear() {
		GATES.clear();
		RECENT_TRAVEL.clear();
	}
}
