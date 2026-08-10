package com.powers.companion;

import com.powers.PowersBlocks;
import com.powers.item.ArtifactWeaponManager;
import com.powers.item.artifact.ArtifactAlignment;
import com.powers.network.CompanionPackets;
import com.powers.entity.FirstVessel;
import com.powers.player.ArtifactSelectionState;
import com.powers.player.PlayerPowers;
import com.powers.player.SkillSystem;
import com.powers.util.BoundedEntityCandidates;
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
	private static final java.util.Set<UUID> REQUESTED = new java.util.HashSet<>();
	private static final Map<UUID, Session> SESSIONS = new HashMap<>();
	private static final Map<UUID, Integer> LAST_DEATH_AT = new HashMap<>();
	private static final Map<UUID, Integer> LAST_RANK = new HashMap<>();
	private static final Map<UUID, Integer> MILESTONE_AT = new HashMap<>();
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
		int currentRank = PlayerPowers.get(player).darknessLevel();
		Integer previousRank = LAST_RANK.put(owner, currentRank);
		if (previousRank != null && currentRank > previousRank) MILESTONE_AT.put(owner, serverTick);
		boolean eligible = PrivateCompanionRules.eligible(
				SkillSystem.hasDarknessTag(player),
				ArtifactWeaponManager.carries(player, ArtifactAlignment.DARKNESS),
				player.isAlive() && !player.isRemoved(),
				PlayerPowers.get(player).mindBody() != null, REQUESTED.contains(owner));
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
		if (suppliedSession == -1L) {
			REQUESTED.add(owner.getUUID());
			ELIGIBILITY.put(owner.getUUID(), STABLE_ELIGIBILITY_TICKS);
			return;
		}
		if (suppliedSession == -2L) {
			REQUESTED.remove(owner.getUUID());
			ELIGIBILITY.remove(owner.getUUID());
			despawn(owner);
			return;
		}
		Session session = SESSIONS.get(owner.getUUID());
		if (session == null || !session.dimension.equals(dimension(owner))) return;
		Vec3 eyeToCompanion = session.position.add(0.0, 1.62, 0.0).subtract(owner.getEyePosition());
		double distanceSquared = eyeToCompanion.lengthSqr();
		double viewDot = distanceSquared < 1.0E-6 ? 1.0
				: owner.getLookAngle().dot(eyeToCompanion.normalize());
		if (!PrivateCompanionRules.mayInteract(suppliedSession, session.id,
				distanceSquared, viewDot)) return;
		LoreDialogueContext context = context(owner);
		String fallback = DIALOGUE.line(owner.getUUID(), context, false);
		DialogueProviderRuntime.request(owner.getUUID(), context, false, fallback)
				.thenAccept(line -> owner.level().getServer().execute(() -> sendDialogueIfCurrent(
						owner, suppliedSession, line)));
	}

	private static void sendDialogueIfCurrent(ServerPlayer owner, long suppliedSession, String line) {
		if (owner.connection == null || owner.isRemoved()) return;
		Session current = SESSIONS.get(owner.getUUID());
		if (current == null || current.id != suppliedSession
				|| !current.dimension.equals(dimension(owner))) return;
		CompanionPackets.sendState(owner, current.id, true, false,
				current.position.x, current.position.y, current.position.z, current.yaw, line);
	}

	private static LoreDialogueContext context(ServerPlayer player) {
		PlayerPowers.PlayerPowersData powers = PlayerPowers.get(player);
		int tick = player.level().getServer().getTickCount();
		String realm = player.level().dimension().identifier().getPath();
		String alignment = nearbyAlignment(player);
		String action = ArtifactSelectionState.selected(player, ArtifactAlignment.DARKNESS);
		boolean bossNearby = !BoundedEntityCandidates.ofClass(player.level(), FirstVessel.class,
				player.getBoundingBox().inflate(64.0), 1, FirstVessel::isAlive).isEmpty();
		boolean recentDeath = tick - LAST_DEATH_AT.getOrDefault(player.getUUID(), -100_000)
				<= 20 * 120;
		boolean milestone = tick - MILESTONE_AT.getOrDefault(player.getUUID(), -100_000)
				<= 20 * 60;
		return new LoreDialogueContext(realm,
				player.getHealth() <= player.getMaxHealth() * 0.35F,
				powers.energy() <= powers.energyCapacity() / 4,
				powers.darknessLevel(), alignment, action,
				recentDeath, bossNearby, milestone ? "rank_" + powers.darknessLevel() : "none");
	}

	/** Records only an owner timestamp; no death location or attacker is retained. */
	public static void recordDeath(ServerPlayer player) {
		LAST_DEATH_AT.put(player.getUUID(), player.level().getServer().getTickCount());
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
		REQUESTED.remove(player.getUUID());
		LAST_DEATH_AT.remove(player.getUUID());
		LAST_RANK.remove(player.getUUID());
		MILESTONE_AT.remove(player.getUUID());
		despawn(player);
		DIALOGUE.forget(player.getUUID());
		DialogueProviderRuntime.forget(player.getUUID());
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
		REQUESTED.clear();
		SESSIONS.clear();
		LAST_DEATH_AT.clear();
		LAST_RANK.clear();
		MILESTONE_AT.clear();
		DIALOGUE.clear();
		DialogueProviderRuntime.clear();
	}
}
