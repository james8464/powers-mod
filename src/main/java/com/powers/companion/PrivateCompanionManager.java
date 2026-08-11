package com.powers.companion;

import com.powers.PowersSounds;
import com.powers.fx.PowerFx;
import com.powers.item.ArtifactWeaponManager;
import com.powers.item.artifact.ArtifactAlignment;
import com.powers.knowledge.KnowledgeAnswer;
import com.powers.knowledge.KnowledgeService;
import com.powers.magic.runtime.MagicLifecycleRules;
import com.powers.network.CompanionPackets;
import com.powers.player.PlayerPowers;
import com.powers.player.SkillSystem;
import com.powers.power.state.PowerEntityState;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Owns lightweight Shadow sessions without AI, equipment, persistence, or
 * chunk tickets. Hidden sessions render an owner-only client apparition;
 * revealed sessions replace it with one globally tracked mortal mannequin.
 */
public final class PrivateCompanionManager {
	private static final AtomicLong NEXT_SESSION = new AtomicLong(1L);
	private static final Set<UUID> REQUESTED = new HashSet<>();
	private static final Set<UUID> REVEALED = new HashSet<>();
	private static final Map<UUID, Session> SESSIONS = new HashMap<>();
	private static final Map<UUID, UUID> BODY_OWNERS = new HashMap<>();

	private static final class Session {
		private final long id;
		private String dimension;
		private Vec3 position;
		private float yaw;
		private boolean revealed;
		private Mannequin body;
		private final Set<UUID> viewers = new HashSet<>();
		private final Set<UUID> pendingTeleports = new HashSet<>();
		private long viewerCursor;

		private Session(long id, String dimension, Vec3 position, float yaw, boolean revealed) {
			this.id = id;
			this.dimension = dimension;
			this.position = position;
			this.yaw = yaw;
			this.revealed = revealed;
		}
	}

	private PrivateCompanionManager() {
	}

	public static void tickPlayer(ServerPlayer player, int serverTick) {
		UUID owner = player.getUUID();
		if (!eligible(player)) {
			REQUESTED.remove(owner);
			REVEALED.remove(owner);
			despawn(player);
			return;
		}
		if (!REQUESTED.contains(owner)) {
			despawn(player);
			return;
		}

		Session session = SESSIONS.get(owner);
		if (session == null) {
			Vec3 position = desiredPosition(player);
			session = new Session(NEXT_SESSION.getAndIncrement(), dimension(player),
					position, player.getYRot(), REVEALED.contains(owner));
			SESSIONS.put(owner, session);
			syncViewers(player, session, true);
			return;
		}
		if (!CompanionSyncRules.shouldUpdate(serverTick, session.id)) return;
		if (session.revealed && (session.body == null || !session.body.isAlive()
				|| session.body.isRemoved())) {
			ServerLevel effectLevel = session.body != null
					&& session.body.level() instanceof ServerLevel bodyLevel
							? bodyLevel : (ServerLevel) player.level();
			dismissBrokenBody(player, session, effectLevel,
					session.body == null ? player.position() : session.body.position());
			return;
		}

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
		session.revealed = REVEALED.contains(owner);
		syncViewers(player, session, teleport);
	}

	/** Item/body/death eligibility is reconciled even when the global clock is frozen. */
	public static void reconcileEligibility(ServerPlayer player) {
		if (eligible(player)) return;
		REQUESTED.remove(player.getUUID());
		REVEALED.remove(player.getUUID());
		despawn(player);
	}

	/**
	 * Consumes an explicit Shadow address so it never appears as ordinary chat.
	 * Returns false for all unrelated messages.
	 */
	public static boolean handleChat(ServerPlayer owner, String rawMessage) {
		ShadowChatIntent intent = ShadowChatIntent.parse(rawMessage);
		if (!intent.addressed()) return false;
		if (!eligible(owner)) {
			owner.sendSystemMessage(Component.literal(
					"Shadow is silent. Darkness and the Shadow Sword must both recognise you.")
					.withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
			return true;
		}
		switch (intent.action()) {
			case EMPTY -> reply(owner, "Speak, and I will listen.");
			case TOO_LONG -> reply(owner, "One thought at a time. Your question is too long.");
			case SUMMON -> {
				REQUESTED.add(owner.getUUID());
				reply(owner, "I am beside you.");
			}
			case DISMISS -> {
				REQUESTED.remove(owner.getUUID());
				REVEALED.remove(owner.getUUID());
				despawn(owner);
				replyPrivate(owner, "I return to the blade.");
			}
			case REVEAL -> {
				REQUESTED.add(owner.getUUID());
				REVEALED.add(owner.getUUID());
				setRevealed(owner, true);
				reply(owner, "Let every witness see what follows you.");
			}
			case HIDE -> {
				REQUESTED.add(owner.getUUID());
				REVEALED.remove(owner.getUUID());
				setRevealed(owner, false);
				replyPrivate(owner, "Only you may see or hear me now.");
			}
			case QUESTION -> {
				REQUESTED.add(owner.getUUID());
				answer(owner, intent.message());
			}
			case NONE -> {
				return false;
			}
		}
		return true;
	}

	/** G-key compatibility: summon or dismiss without creating a chat message. */
	public static void interact(ServerPlayer owner, long request) {
		if (!eligible(owner)) return;
		if (request == -1L) {
			REQUESTED.add(owner.getUUID());
			reply(owner, "I am beside you.");
		} else if (request == -2L) {
			REQUESTED.remove(owner.getUUID());
			REVEALED.remove(owner.getUUID());
			despawn(owner);
			replyPrivate(owner, "I return to the blade.");
		}
	}

	private static void answer(ServerPlayer owner, String question) {
		KnowledgeService.answerAsync(owner, question).thenAccept(answer ->
				owner.level().getServer().execute(() -> {
					if (owner.connection == null || owner.isRemoved()
							|| !REQUESTED.contains(owner.getUUID())) return;
					reply(owner, spokenAnswer(answer));
				}));
	}

	private static String spokenAnswer(KnowledgeAnswer answer) {
		String text = answer.answer().strip();
		if (!text.isEmpty()) return text;
		return "That truth has not yet left a trace I can verify.";
	}

	private static void setRevealed(ServerPlayer owner, boolean revealed) {
		Session session = SESSIONS.get(owner.getUUID());
		if (session == null) return;
		session.revealed = revealed;
		syncViewers(owner, session, true);
	}

	private static void syncViewers(ServerPlayer owner, Session session, boolean teleport) {
		List<ServerPlayer> desired = new ArrayList<>();
		if (session.revealed) {
			if (!ensureRevealedBody(owner, session)) {
				dismissBrokenBody(owner, session, (ServerLevel) owner.level(), session.position);
				return;
			}
			updateBodyTransform(session, teleport);
		} else {
			discardBody(session);
			desired.add(owner);
		}
		Set<UUID> desiredIds = new HashSet<>();
		for (ServerPlayer player : desired) desiredIds.add(player.getUUID());
		if (teleport) session.pendingTeleports.addAll(session.viewers);
		for (UUID previous : Set.copyOf(session.viewers)) {
			if (desiredIds.contains(previous)) continue;
			ServerPlayer recipient = owner.level().getServer().getPlayerList().getPlayer(previous);
			if (recipient != null && recipient.connection != null) {
				CompanionPackets.sendCriticalState(recipient, owner.getUUID(), session.id, false, true,
						session.dimension, 0.0, 0.0, 0.0, 0.0F);
			}
			session.viewers.remove(previous);
			session.pendingTeleports.remove(previous);
		}
		int allowance = Math.min(desired.size(),
				CompanionSyncRules.viewerAllowance(SESSIONS.size()));
		for (int offset = 0; offset < allowance; offset++) {
			int index = CompanionSyncRules.rotatingIndex(session.viewerCursor + offset,
					desired.size());
			ServerPlayer recipient = desired.get(index);
			UUID recipientId = recipient.getUUID();
			boolean recipientTeleport = teleport
					|| session.pendingTeleports.contains(recipientId)
					|| !session.viewers.contains(recipientId);
			if (CompanionPackets.sendState(recipient, owner.getUUID(), session.id, true,
					recipientTeleport,
					session.dimension, session.position.x, session.position.y,
					session.position.z, session.yaw)) {
				session.viewers.add(recipientId);
				session.pendingTeleports.remove(recipientId);
			}
		}
		session.viewerCursor += allowance;
	}

	private static boolean ensureRevealedBody(ServerPlayer owner, Session session) {
		if (session.body != null && session.body.isAlive() && !session.body.isRemoved()
				&& session.body.level() == owner.level()) return true;
		discardBody(session);
		ServerLevel level = (ServerLevel) owner.level();
		Mannequin body = EntityTypes.MANNEQUIN.create(level, EntitySpawnReason.TRIGGERED);
		if (body == null) return false;
		body.setPos(session.position);
		body.setYRot(session.yaw);
		body.setYHeadRot(session.yaw);
		body.setYBodyRot(session.yaw);
		body.setNoGravity(true);
		body.setSilent(true);
		body.setInvulnerable(false);
		body.setCustomName(Component.literal("Shadow of " + owner.getScoreboardName()));
		body.setCustomNameVisible(false);
		body.setComponent(DataComponents.PROFILE,
				ResolvableProfile.createResolved(owner.getGameProfile()));
		PowerEntityState.markEphemeral(body);
		if (!level.addFreshEntity(body)) return false;
		session.body = body;
		BODY_OWNERS.put(body.getUUID(), owner.getUUID());
		manifestBody(level, body.position());
		return true;
	}

	private static void manifestBody(ServerLevel level, Vec3 position) {
		PowerFx.rune(level, position, 1.3, 0x55265F, 24, 0.0);
		PowerFx.burst(level, position.add(0.0, 0.9, 0.0),
				ParticleTypes.REVERSE_PORTAL, 18, 0.55, 0.02);
	}

	private static void updateBodyTransform(Session session, boolean teleport) {
		Mannequin body = session.body;
		if (body == null) return;
		Vec3 previous = body.position();
		Vec3 next = teleport ? session.position : previous.lerp(session.position, 0.55);
		body.setDeltaMovement(next.subtract(previous));
		body.setPos(next);
		body.setYRot(session.yaw);
		body.setYHeadRot(session.yaw);
		body.setYBodyRot(session.yaw);
		body.setNoGravity(true);
	}

	private static void discardBody(Session session) {
		Mannequin body = session.body;
		if (body == null) return;
		BODY_OWNERS.remove(body.getUUID());
		if (!body.isRemoved()) body.discard();
		session.body = null;
	}

	/** Called by the common death hook; memories remain keyed to the sword owner. */
	public static boolean afterDeath(LivingEntity entity) {
		UUID ownerId = entity == null ? null : BODY_OWNERS.get(entity.getUUID());
		if (ownerId == null) return false;
		if (MagicLifecycleRules.resolve(MagicLifecycleRules.Form.SHADOW_REVEALED,
				MagicLifecycleRules.Source.SHADOW_SWORD,
				MagicLifecycleRules.Event.AVATAR_FATAL).outcome()
				!= MagicLifecycleRules.Outcome.DISMISS_SHADOW) return false;
		Session session = SESSIONS.get(ownerId);
		if (session == null || session.body != entity) {
			BODY_OWNERS.remove(entity.getUUID(), ownerId);
			return false;
		}
		BODY_OWNERS.remove(entity.getUUID(), ownerId);
		ServerPlayer owner = entity.level().getServer().getPlayerList().getPlayer(ownerId);
		if (owner != null) dismissBrokenBody(owner, session,
				(ServerLevel) entity.level(), entity.position());
		else {
			session.body = null;
			SESSIONS.remove(ownerId);
			REQUESTED.remove(ownerId);
			REVEALED.remove(ownerId);
		}
		return true;
	}

	private static void dismissBrokenBody(ServerPlayer owner, Session session,
			ServerLevel effectLevel, Vec3 position) {
		REQUESTED.remove(owner.getUUID());
		REVEALED.remove(owner.getUUID());
		SESSIONS.remove(owner.getUUID(), session);
		removeClientViewers(owner, session);
		discardBody(session);
		PowerFx.rune(effectLevel, position, 1.4, 0x55265F, 24, Math.PI);
		PowerFx.burst(effectLevel, position.add(0.0, 0.9, 0.0),
				ParticleTypes.REVERSE_PORTAL, 26, 0.7, 0.03);
		PowerFx.sound(effectLevel, position, PowersSounds.DARK_WHISPER, 1.1F, 0.55F);
		replyPrivate(owner, "This vessel is broken. Call me from the blade, and I will remember.");
	}

	private static void removeClientViewers(ServerPlayer owner, Session session) {
		for (UUID viewer : Set.copyOf(session.viewers)) {
			ServerPlayer recipient = owner.level().getServer().getPlayerList().getPlayer(viewer);
			if (recipient != null && recipient.connection != null) {
				CompanionPackets.sendCriticalState(recipient, owner.getUUID(), session.id, false, true,
						session.dimension, 0.0, 0.0, 0.0, 0.0F);
			}
		}
		session.viewers.clear();
		session.pendingTeleports.clear();
	}

	private static void reply(ServerPlayer owner, String line) {
		List<UUID> online = owner.level().getServer().getPlayerList().getPlayers().stream()
				.map(ServerPlayer::getUUID).toList();
		for (UUID id : PrivateCompanionRules.recipients(owner.getUUID(), online,
				REVEALED.contains(owner.getUUID()))) {
			ServerPlayer recipient = owner.level().getServer().getPlayerList().getPlayer(id);
			if (recipient != null) sendReply(recipient, owner, line);
		}
	}

	private static void replyPrivate(ServerPlayer owner, String line) {
		sendReply(owner, owner, line);
	}

	private static void sendReply(ServerPlayer recipient, ServerPlayer owner, String line) {
		Component prefix = Component.literal("Shadow of " + owner.getScoreboardName() + ": ")
				.withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD);
		recipient.sendSystemMessage(prefix.copy().append(
				Component.literal(line).withStyle(ChatFormatting.GRAY)));
	}

	private static boolean eligible(ServerPlayer player) {
		return PrivateCompanionRules.eligible(
				SkillSystem.hasDarknessTag(player),
				ArtifactWeaponManager.carries(player, ArtifactAlignment.DARKNESS),
				player.isAlive() && !player.isRemoved(),
				PlayerPowers.get(player).mindBody() != null, true);
	}

	private static Vec3 desiredPosition(ServerPlayer player) {
		return PrivateCompanionRules.followPoint(player.position(), player.getLookAngle());
	}

	private static String dimension(ServerPlayer player) {
		return player.level().dimension().identifier().toString();
	}

	/** Bounded diagnostics for GameTests and {@code /powers diagnose}. */
	public static int activeSessionCount() {
		return SESSIONS.size();
	}

	/** Number of mortal, globally tracked Shadow bodies (hidden sessions add none). */
	public static int activeRevealedBodyCount() {
		return BODY_OWNERS.size();
	}

	/** Stable world-entity identity for diagnostics and live verification. */
	public static Optional<UUID> revealedBodyId(UUID owner) {
		Session session = SESSIONS.get(owner);
		return session == null || session.body == null ? Optional.empty()
				: Optional.of(session.body.getUUID());
	}

	/** Returns the current server-authoritative global visibility state. */
	public static boolean isRevealed(UUID owner) {
		return REVEALED.contains(owner);
	}

	public static void forget(ServerPlayer player) {
		REQUESTED.remove(player.getUUID());
		REVEALED.remove(player.getUUID());
		despawn(player);
		com.powers.knowledge.KnowledgeRemoteProviderRuntime.forget(player.getUUID());
		com.powers.knowledge.MagicAttemptJournal.global().forget(player.getUUID());
	}

	private static void despawn(ServerPlayer owner) {
		Session removed = SESSIONS.remove(owner.getUUID());
		if (removed == null) return;
		removeClientViewers(owner, removed);
		discardBody(removed);
	}

	public static void clear() {
		for (Session session : List.copyOf(SESSIONS.values())) discardBody(session);
		REQUESTED.clear();
		REVEALED.clear();
		SESSIONS.clear();
		BODY_OWNERS.clear();
		com.powers.knowledge.KnowledgeRemoteProviderRuntime.clear();
		com.powers.knowledge.MagicAttemptJournal.global().clear();
	}
}
