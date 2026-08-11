package com.powers.companion;

import com.powers.item.ArtifactWeaponManager;
import com.powers.item.artifact.ArtifactAlignment;
import com.powers.knowledge.KnowledgeAnswer;
import com.powers.knowledge.KnowledgeService;
import com.powers.network.CompanionPackets;
import com.powers.player.PlayerPowers;
import com.powers.player.SkillSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Owns Shadow sessions without creating server entities, AI, hitboxes, or
 * chunk tickets. Each authorised client renders a local player-shaped shell;
 * hidden sessions are sent only to their owner and revealed sessions are sent
 * to every player in the owner's current dimension.
 */
public final class PrivateCompanionManager {
	private static final int UPDATE_INTERVAL_TICKS = 4;
	private static final AtomicLong NEXT_SESSION = new AtomicLong(1L);
	private static final Set<UUID> REQUESTED = new HashSet<>();
	private static final Set<UUID> REVEALED = new HashSet<>();
	private static final Map<UUID, Session> SESSIONS = new HashMap<>();

	private static final class Session {
		private final long id;
		private String dimension;
		private Vec3 position;
		private float yaw;
		private boolean revealed;
		private final Set<UUID> viewers = new HashSet<>();

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
		session.revealed = REVEALED.contains(owner);
		syncViewers(player, session, teleport);
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
		List<ServerPlayer> desired = session.revealed
				? owner.level().players().stream().filter(player -> player.connection != null).toList()
				: List.of(owner);
		Set<UUID> desiredIds = desired.stream().map(ServerPlayer::getUUID)
				.collect(java.util.stream.Collectors.toUnmodifiableSet());
		for (UUID previous : Set.copyOf(session.viewers)) {
			if (desiredIds.contains(previous)) continue;
			ServerPlayer recipient = owner.level().getServer().getPlayerList().getPlayer(previous);
			if (recipient != null && recipient.connection != null) {
				CompanionPackets.sendState(recipient, owner.getUUID(), session.id, false, true,
						session.dimension, 0.0, 0.0, 0.0, 0.0F);
			}
		}
		for (ServerPlayer recipient : desired) {
			CompanionPackets.sendState(recipient, owner.getUUID(), session.id, true, teleport,
					session.dimension, session.position.x, session.position.y,
					session.position.z, session.yaw);
		}
		session.viewers.clear();
		session.viewers.addAll(desiredIds);
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

	/** Returns the current server-authoritative global visibility state. */
	public static boolean isRevealed(UUID owner) {
		return REVEALED.contains(owner);
	}

	public static void forget(ServerPlayer player) {
		REQUESTED.remove(player.getUUID());
		REVEALED.remove(player.getUUID());
		despawn(player);
		com.powers.knowledge.KnowledgeRemoteProviderRuntime.forget(player.getUUID());
	}

	private static void despawn(ServerPlayer owner) {
		Session removed = SESSIONS.remove(owner.getUUID());
		if (removed == null) return;
		for (ServerPlayer recipient : owner.level().getServer().getPlayerList().getPlayers()) {
			if (recipient.connection != null) {
				CompanionPackets.sendState(recipient, owner.getUUID(), removed.id, false, true,
						removed.dimension, 0.0, 0.0, 0.0, 0.0F);
			}
		}
	}

	public static void clear() {
		REQUESTED.clear();
		REVEALED.clear();
		SESSIONS.clear();
		com.powers.knowledge.KnowledgeRemoteProviderRuntime.clear();
	}
}
