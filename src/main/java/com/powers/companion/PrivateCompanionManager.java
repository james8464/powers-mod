package com.powers.companion;

import com.powers.PowersBlocks;
import com.powers.item.ArtifactWeaponManager;
import com.powers.item.artifact.ArtifactAlignment;
import com.powers.network.CompanionPackets;
import com.powers.player.ArtifactSelectionState;
import com.powers.player.PlayerPowers;
import com.powers.player.SkillSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Owns lightweight server sessions for owner-private companion apparitions.
 * No companion entity exists on the server, so this has constant work per
 * eligible owner and cannot add mob AI, collision, tracking, or chunk tickets.
 */
public final class PrivateCompanionManager {
	private static final int STABLE_ELIGIBILITY_TICKS = 40;
	private static final int UPDATE_INTERVAL_TICKS = 4;
	private static final AtomicLong NEXT_SESSION = new AtomicLong(1L);
	private static final Map<UUID, Integer> ELIGIBILITY = new HashMap<>();
	private static final Map<UUID, Session> SESSIONS = new HashMap<>();
	private static final LoreDialogueEngine DIALOGUE = new LoreDialogueEngine();

	private static final class Session {
		private final long id;
		private String dimension;
		private Vec3 position;
		private float yaw;

		private Session(long id, String dimension, Vec3 position, float yaw) {
			this.id = id;
			this.dimension = dimension;
			this.position = position;
			this.yaw = yaw;
		}
	}

	private PrivateCompanionManager() {
	}

	public static void tickPlayer(ServerPlayer player, int serverTick) {
		UUID owner = player.getUUID();
		boolean eligible = PrivateCompanionRules.eligible(
				SkillSystem.hasDarknessTag(player),
				ArtifactWeaponManager.carries(player, ArtifactAlignment.DARKNESS),
				player.isAlive() && !player.isRemoved(),
				PlayerPowers.get(player).mindBody() != null);
		if (!eligible) {
			ELIGIBILITY.remove(owner);
			despawn(player);
			return;
		}

		Session session = SESSIONS.get(owner);
		if (session == null) {
			int stableTicks = ELIGIBILITY.merge(owner, 1, Integer::sum);
			if (stableTicks < STABLE_ELIGIBILITY_TICKS) return;
			Vec3 position = desiredPosition(player);
			session = new Session(NEXT_SESSION.getAndIncrement(), dimension(player),
					position, player.getYRot());
			SESSIONS.put(owner, session);
			CompanionPackets.sendState(player, session.id, true, true,
					position.x, position.y, position.z, session.yaw, "");
			return;
		}
		if (serverTick % UPDATE_INTERVAL_TICKS != 0) return;

		String dimension = dimension(player);
		Vec3 desired = desiredPosition(player);
		boolean teleport = !dimension.equals(session.dimension)
				|| PrivateCompanionRules.shouldTeleport(session.position, desired);
		if (teleport) {
			session.position = desired;
			session.dimension = dimension;
		} else {
			session.position = session.position.lerp(desired, 0.55);
		}
		session.yaw = player.getYRot();
		CompanionPackets.sendState(player, session.id, true, teleport,
				session.position.x, session.position.y, session.position.z, session.yaw, "");
	}

	/** Validates the private session, distance, and view cone before speaking. */
	public static void interact(ServerPlayer owner, long suppliedSession) {
		Session session = SESSIONS.get(owner.getUUID());
		if (session == null || !session.dimension.equals(dimension(owner))) return;
		Vec3 eyeToCompanion = session.position.add(0.0, 1.62, 0.0).subtract(owner.getEyePosition());
		double distanceSquared = eyeToCompanion.lengthSqr();
		double viewDot = distanceSquared < 1.0E-6 ? 1.0
				: owner.getLookAngle().dot(eyeToCompanion.normalize());
		if (!PrivateCompanionRules.mayInteract(suppliedSession, session.id,
				distanceSquared, viewDot)) return;
		String line = DIALOGUE.line(owner.getUUID(), context(owner), false);
		CompanionPackets.sendState(owner, session.id, true, false,
				session.position.x, session.position.y, session.position.z, session.yaw, line);
	}

	private static LoreDialogueContext context(ServerPlayer player) {
		PlayerPowers.PlayerPowersData powers = PlayerPowers.get(player);
		String realm = player.level().dimension().identifier().getPath();
		String alignment = nearbyAlignment(player);
		String action = ArtifactSelectionState.selected(player, ArtifactAlignment.DARKNESS);
		return new LoreDialogueContext(realm,
				player.getHealth() <= player.getMaxHealth() * 0.35F,
				powers.energy() <= powers.energyCapacity() / 4,
				powers.darknessLevel(), alignment, action,
				false, false, "none");
	}

	/** Samples a fixed 7-cube instead of searching arbitrary loaded chunks. */
	private static String nearbyAlignment(ServerPlayer player) {
		BlockPos center = player.blockPosition();
		for (BlockPos pos : BlockPos.betweenClosed(center.offset(-3, -3, -3),
				center.offset(3, 3, 3))) {
			var block = player.level().getBlockState(pos).getBlock();
			if (block == PowersBlocks.DARKNESS) return "darkness";
			if (block == PowersBlocks.PURE_LIGHT) return "pure_light";
		}
		return "none";
	}

	private static Vec3 desiredPosition(ServerPlayer player) {
		return PrivateCompanionRules.followPoint(player.position(), player.getLookAngle());
	}

	private static String dimension(ServerPlayer player) {
		return player.level().dimension().identifier().toString();
	}

	public static void forget(ServerPlayer player) {
		ELIGIBILITY.remove(player.getUUID());
		despawn(player);
		DIALOGUE.forget(player.getUUID());
	}

	private static void despawn(ServerPlayer player) {
		Session removed = SESSIONS.remove(player.getUUID());
		if (removed != null && player.connection != null) {
			CompanionPackets.sendState(player, removed.id, false, true,
					0.0, 0.0, 0.0, 0.0F, "");
		}
	}

	public static void clear() {
		ELIGIBILITY.clear();
		SESSIONS.clear();
		DIALOGUE.clear();
	}
}
