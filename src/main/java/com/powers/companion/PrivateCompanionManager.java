package com.powers.companion;

import com.powers.PowersEntities;
import com.powers.PowersSounds;
import com.powers.fx.PowerFx;
import com.powers.item.ArtifactWeaponManager;
import com.powers.item.artifact.ArtifactAlignment;
import com.powers.magic.runtime.MagicLifecycleRules;
import com.powers.network.CompanionPackets;
import com.powers.player.PlayerPowers;
import com.powers.player.SkillSystem;
import com.powers.companion.combat.ShadowPowerRuntime;
import com.powers.companion.combat.ShadowCombatController;
import com.powers.util.LoadedChunks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/** Owns one real Shadow body and its private-apparition presentation per eligible player. */
public final class PrivateCompanionManager {
	private static final AtomicLong NEXT_SESSION = new AtomicLong(1L);
	private static final Set<UUID> REQUESTED = new HashSet<>();
	private static final Set<UUID> REVEALED = new HashSet<>();
	private static final Map<UUID, Session> SESSIONS = new HashMap<>();
	private static final Map<UUID, UUID> BODY_OWNERS = new HashMap<>();

	static final class Session {
		private final long id;
		ShadowCompanionEntity body;
		private final Set<UUID> apparitionViewers = new HashSet<>();
		final ShadowTaskController tasks = new ShadowTaskController();
		private int dimensionTransferFailures;

		private Session(long id, ShadowCompanionEntity body) {
			this.id = id;
			this.body = body;
		}
	}

	private PrivateCompanionManager() {
	}

	public static void tickPlayer(ServerPlayer player, int serverTick) {
		UUID ownerId = player.getUUID();
		if (!eligible(player)) {
			REQUESTED.remove(ownerId);
			REVEALED.remove(ownerId);
			despawn(player);
			return;
		}
		if (!REQUESTED.contains(ownerId)) {
			despawn(player);
			return;
		}
		ShadowCompanionData data = ShadowCompanionStore.get(player);
		if (!ShadowStatusSync.mayProceed(player, data)) return;

		Session session = SESSIONS.get(ownerId);
		if (session == null) {
			ShadowCompanionEntity body = ensureBody(player, data);
			if (body == null) return;
			session = new Session(NEXT_SESSION.getAndIncrement(), body);
			SESSIONS.put(ownerId, session);
		}
		if (!CompanionSyncRules.shouldUpdate(serverTick, session.id)) return;
		if (session.body == null || !session.body.isAlive() || session.body.isRemoved()) {
			dismissBrokenBody(player, session,
					session.body == null ? player.position() : session.body.position());
			return;
		}

		boolean revealed = REVEALED.contains(ownerId);
		if (session.body.revealed() != revealed) session.body.setRevealed(revealed);
		ShadowCompanionEntity body = session.body;
		ShadowCompanionStore.update(player, current -> current
				.withEnergy(body.energy()).withRevealed(revealed)
				.withBodyId(body.getUUID()));
		ShadowMagicState.tick(player, body);
		ShadowPowerRuntime.tick(player, body, player.level().getServer().getTickCount());
		if (!ShadowConjurationManager.active(ownerId)) {
			ShadowCompanionData combatData = ShadowCompanionStore.get(player);
			var combat = ShadowCombatController.tick((ServerLevel) body.level(), body, player,
					serverTick, session.id, combatData.preferredCombatRange(),
					combatData.learnedCombat());
			if (!combat.learnedState().isEmpty()) {
				ShadowCompanionStore.update(player, state -> state.withLearnedCombat(
						combat.learnedState()));
			}
		}
		ShadowTask.Result taskState = session.tasks.tick(player.level().getGameTime());
		if (taskState.state() == ShadowTask.State.FAILED) {
			ShadowCompanionMessaging.rememberFailure(player, taskState.reason());
			ShadowCompanionMessaging.replyAndRemember(
					player, "", ShadowCommandRuntime.failure(taskState.reason()));
		}
		if (ShadowConjurationManager.active(ownerId)) {
			ShadowCommandRuntime.tickConjuration(player, session);
		}
		ShadowStatusSync.active(player, body);
		boolean dimensionMismatch = body.level() != player.level();
		boolean teleported = (dimensionMismatch
				|| ShadowCompanionStore.get(player).stance() == ShadowStance.FOLLOW)
				&& followOwner(player, session);
		if (dimensionMismatch && session.body.level() != player.level()) {
			session.dimensionTransferFailures++;
			if (session.dimensionTransferFailures >= 3) {
				ShadowCompanionMessaging.replyPrivate(player,
						"The path between dimensions failed safely; call me again once it is stable.");
				despawn(player);
				return;
			}
		} else {
			session.dimensionTransferFailures = 0;
		}
		syncApparition(player, session, teleported);
	}

	/** Item/body/death eligibility is reconciled even while the global clock is frozen. */
	public static void reconcileEligibility(ServerPlayer player) {
		if (eligible(player)) return;
		REQUESTED.remove(player.getUUID());
		REVEALED.remove(player.getUUID());
		despawn(player);
	}

	/** Consumes explicit Shadow-addressed chat and leaves unrelated signed chat untouched. */
	public static boolean handleChat(ServerPlayer owner, String rawMessage) {
		return ShadowCommandRuntime.handle(owner, rawMessage);
	}

	static void request(ServerPlayer owner) {
		REQUESTED.add(owner.getUUID());
	}

	static boolean requested(UUID owner) {
		return REQUESTED.contains(owner);
	}

	static Session session(UUID owner) {
		return SESSIONS.get(owner);
	}

	static void dismiss(ServerPlayer owner) {
		REQUESTED.remove(owner.getUUID());
		REVEALED.remove(owner.getUUID());
		despawn(owner);
	}

	static void setVisibility(ServerPlayer owner, boolean revealed) {
		REQUESTED.add(owner.getUUID());
		if (revealed) REVEALED.add(owner.getUUID());
		else REVEALED.remove(owner.getUUID());
		setRevealed(owner, revealed);
	}

	/** G-key compatibility: summon or dismiss without creating a chat message. */
	public static void interact(ServerPlayer owner, long request) {
		if (!eligible(owner)) return;
		if (request == -1L) summon(owner);
		else if (request == -2L) {
			REQUESTED.remove(owner.getUUID());
			REVEALED.remove(owner.getUUID());
			despawn(owner);
			ShadowCompanionMessaging.replyPrivate(owner, "I return to the blade.");
		}
	}

	static void summon(ServerPlayer owner) {
		ShadowCompanionData data = ShadowCompanionStore.get(owner);
		long now = owner.level().getGameTime();
		if (!ShadowManifestationRules.mayRecall(data, now)) {
			ShadowCompanionMessaging.replyPrivate(owner,
					"The blade is still rebuilding what was broken.");
			return;
		}
		REQUESTED.add(owner.getUUID());
		if (data.revealed()) REVEALED.add(owner.getUUID());
		ShadowCompanionMessaging.reply(owner, "I am beside you.");
	}

	private static ShadowCompanionEntity ensureBody(ServerPlayer owner,
			ShadowCompanionData data) {
		ShadowCompanionEntity body = data.bodyUuid().flatMap(id -> findBody(owner, id)).orElse(null);
		if (body == null) {
			ServerLevel level = (ServerLevel) owner.level();
			body = PowersEntities.SHADOW_COMPANION.create(level, EntitySpawnReason.TRIGGERED);
			if (body == null) return null;
			body.setPos(PrivateCompanionRules.followPoint(owner.position(), owner.getLookAngle()));
			body.configure(owner, data.withRevealed(REVEALED.contains(owner.getUUID())));
			if (!level.addFreshEntity(body)) return null;
			manifestBody(level, body.position());
		} else {
			body.configure(owner, data.withRevealed(REVEALED.contains(owner.getUUID())));
		}
		BODY_OWNERS.put(body.getUUID(), owner.getUUID());
		ShadowCompanionEntity resolved = body;
		ShadowCompanionStore.update(owner, current -> current.withBodyId(resolved.getUUID()));
		return body;
	}

	private static Optional<ShadowCompanionEntity> findBody(ServerPlayer owner, UUID bodyId) {
		for (ServerLevel level : owner.level().getServer().getAllLevels()) {
			Entity entity = level.getEntity(bodyId);
			if (entity instanceof ShadowCompanionEntity body
					&& owner.getUUID().equals(body.ownerId()) && body.isAlive()) return Optional.of(body);
		}
		return Optional.empty();
	}

	private static boolean followOwner(ServerPlayer owner, Session session) {
		ShadowCompanionEntity body = session.body;
		Vec3 desired = PrivateCompanionRules.followPoint(owner.position(), owner.getLookAngle());
		boolean changedDimension = body.level() != owner.level();
		boolean tooFar = changedDimension || ShadowCompanionRules.shouldTeleport(
				body.position().distanceToSqr(desired));
		if (!tooFar) return false;
		ServerLevel destinationLevel = (ServerLevel) owner.level();
		BlockPos destinationBlock = BlockPos.containing(desired);
		if (!LoadedChunks.contains(destinationLevel, destinationBlock)) return false;
		AABB landing = body.getBoundingBox().move(desired.subtract(body.position()));
		if (!destinationLevel.getWorldBorder().isWithinBounds(landing)
				|| !destinationLevel.noBlockCollision(body, landing)) {
			desired = owner.position();
		}
		if (changedDimension) {
			Entity moved = body.teleport(new TeleportTransition(destinationLevel, desired, Vec3.ZERO,
					owner.getYRot(), owner.getXRot(), TeleportTransition.PLAY_PORTAL_SOUND));
			if (!(moved instanceof ShadowCompanionEntity movedShadow)) return false;
			BODY_OWNERS.remove(body.getUUID());
			session.body = movedShadow;
			BODY_OWNERS.put(movedShadow.getUUID(), owner.getUUID());
		} else {
			body.setPos(desired);
			body.setDeltaMovement(Vec3.ZERO);
		}
		return true;
	}

	private static void setRevealed(ServerPlayer owner, boolean revealed) {
		Session session = SESSIONS.get(owner.getUUID());
		if (session != null && session.body != null) {
			ShadowManifestationRules.visibility(session.body.getUUID(),
					session.body.revealed(), revealed);
			session.body.setRevealed(revealed);
			syncApparition(owner, session, true);
		}
		ShadowCompanionStore.update(owner, state -> state.withRevealed(revealed));
	}

	private static void syncApparition(ServerPlayer owner, Session session, boolean teleport) {
		ShadowCompanionEntity body = session.body;
		if (body == null) return;
		if (body.revealed()) {
			removeClientViewers(owner, session);
			return;
		}
		if (CompanionPackets.sendState(owner, owner.getUUID(), session.id, true, teleport,
				body.level().dimension().identifier().toString(), body.getX(), body.getY(), body.getZ(),
				body.getYRot())) {
			session.apparitionViewers.add(owner.getUUID());
		}
	}

	/** Called by the common death hook; memories remain on the sword owner. */
	public static boolean afterDeath(LivingEntity entity) {
		if (!(entity instanceof ShadowCompanionEntity body)) return false;
		UUID ownerId = BODY_OWNERS.getOrDefault(body.getUUID(), body.ownerId());
		if (ownerId == null) return false;
		if (MagicLifecycleRules.resolve(MagicLifecycleRules.Form.SHADOW_REVEALED,
				MagicLifecycleRules.Source.SHADOW_SWORD,
				MagicLifecycleRules.Event.AVATAR_FATAL).outcome()
				!= MagicLifecycleRules.Outcome.DISMISS_SHADOW) return false;
		BODY_OWNERS.remove(body.getUUID());
		ShadowCombatController.clearBody(body.getUUID());
		ShadowConjurationManager.abandon(ownerId);
		Session session = SESSIONS.remove(ownerId);
		REQUESTED.remove(ownerId);
		REVEALED.remove(ownerId);
		ServerPlayer owner = entity.level().getServer().getPlayerList().getPlayer(ownerId);
		if (owner != null) {
			if (session != null) removeClientViewers(owner, session);
			ShadowPowerRuntime.clearOwner(owner, body);
			ShadowCompanionStore.set(owner, ShadowManifestationRules.afterDeath(
					ShadowCompanionStore.get(owner), owner.level().getGameTime()));
			collapse((ServerLevel) entity.level(), entity.position());
			ShadowCompanionMessaging.replyPrivate(owner,
					"This vessel is broken. Call me from the blade, and I will remember.");
		}
		return true;
	}

	private static void dismissBrokenBody(ServerPlayer owner, Session session, Vec3 position) {
		REQUESTED.remove(owner.getUUID());
		REVEALED.remove(owner.getUUID());
		SESSIONS.remove(owner.getUUID(), session);
		removeClientViewers(owner, session);
		if (session.body != null) BODY_OWNERS.remove(session.body.getUUID());
		if (session.body != null) ShadowCombatController.clearBody(session.body.getUUID());
		if (session.body != null) ShadowPowerRuntime.clearOwner(owner, session.body);
		ShadowConjurationManager.abandon(owner.getUUID());
		ShadowCompanionStore.set(owner, ShadowManifestationRules.afterDeath(
				ShadowCompanionStore.get(owner), owner.level().getGameTime()));
		collapse((ServerLevel) owner.level(), position);
		ShadowCompanionMessaging.replyPrivate(owner,
				"This vessel is broken. Call me from the blade, and I will remember.");
	}

	private static void manifestBody(ServerLevel level, Vec3 position) {
		PowerFx.rune(level, position, 1.3, 0x55265F, 24, 0.0);
		PowerFx.burst(level, position.add(0.0, 0.9, 0.0),
				com.powers.PowersParticles.ECLIPSE, 18, 0.55, 0.02);
	}

	private static void collapse(ServerLevel level, Vec3 position) {
		PowerFx.rune(level, position, 1.4, 0x55265F, 24, Math.PI);
		PowerFx.burst(level, position.add(0.0, 0.9, 0.0),
				com.powers.PowersParticles.ECLIPSE, 26, 0.7, 0.03);
		PowerFx.sound(level, position, PowersSounds.DARK_WHISPER, 1.1F, 0.55F);
	}

	private static void removeClientViewers(ServerPlayer owner, Session session) {
		for (UUID viewer : Set.copyOf(session.apparitionViewers)) {
			ServerPlayer recipient = owner.level().getServer().getPlayerList().getPlayer(viewer);
			if (recipient != null && recipient.connection != null) {
				CompanionPackets.sendCriticalState(recipient, owner.getUUID(), session.id, false, true,
						owner.level().dimension().identifier().toString(), 0.0, 0.0, 0.0, 0.0F);
			}
		}
		session.apparitionViewers.clear();
	}

	static boolean eligible(ServerPlayer player) {
		return PrivateCompanionRules.eligible(
				SkillSystem.hasDarknessTag(player),
				ArtifactWeaponManager.carries(player, ArtifactAlignment.DARKNESS),
				player.isAlive() && !player.isRemoved(),
				PlayerPowers.get(player).mindBody() != null, true);
	}

	/** Bounded diagnostics for GameTests and {@code /powers diagnose}. */
	public static int activeSessionCount() {
		return SESSIONS.size();
	}

	public static int activeRevealedBodyCount() {
		return (int) SESSIONS.values().stream().filter(session -> session.body != null
				&& session.body.isAlive() && session.body.revealed()).count();
	}

	public static Optional<UUID> bodyId(UUID owner) {
		Session session = SESSIONS.get(owner);
		return session == null || session.body == null ? Optional.empty()
				: Optional.of(session.body.getUUID());
	}

	public static Optional<ShadowCompanionEntity> body(UUID owner) {
		Session session = SESSIONS.get(owner);
		return session == null || session.body == null ? Optional.empty()
				: Optional.of(session.body);
	}

	public static Optional<UUID> revealedBodyId(UUID owner) {
		Session session = SESSIONS.get(owner);
		return session == null || session.body == null || !session.body.revealed()
				? Optional.empty() : Optional.of(session.body.getUUID());
	}

	public static boolean isRevealed(UUID owner) {
		return REVEALED.contains(owner);
	}

	public static void resetLearning(ServerPlayer owner) {
		ShadowCompanionStore.update(owner, state -> state.withLearnedCombat(""));
		body(owner.getUUID()).ifPresent(shadow -> ShadowCombatController.clearBody(shadow.getUUID()));
	}

	public static ShadowDiagnostics diagnostics() {
		return ShadowDiagnostics.collect(SESSIONS.values(), BODY_OWNERS.keySet());
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
		if (removed == null) {
			ShadowCompanionStore.clearBody(owner);
			ShadowStatusSync.inactive(owner);
			return;
		}
		removeClientViewers(owner, removed);
		if (removed.body != null) {
			if (ShadowConjurationManager.active(owner.getUUID())) {
				ShadowConjurationManager.interrupt(owner, removed.body, "source_lost");
			}
			ShadowPowerRuntime.clearOwner(owner, removed.body);
			BODY_OWNERS.remove(removed.body.getUUID());
			ShadowCombatController.clearBody(removed.body.getUUID());
			ShadowCompanionStore.update(owner, state -> state.withEnergy(removed.body.energy())
					.withRevealed(false).withoutBody());
			if (!removed.body.isRemoved()) removed.body.discard();
		}
		ShadowStatusSync.inactive(owner);
	}

	public static void clear() {
		for (Map.Entry<UUID, Session> entry : Map.copyOf(SESSIONS).entrySet()) {
			Session session = entry.getValue();
			ServerPlayer owner = session.body == null ? null : session.body.level().getServer()
					.getPlayerList().getPlayer(entry.getKey());
			if (session.body != null && ShadowConjurationManager.active(entry.getKey())) {
				if (owner != null) ShadowConjurationManager.interrupt(owner, session.body,
						"server_stopping");
			}
			if (owner != null && session.body != null) ShadowPowerRuntime.clearOwner(owner, session.body);
			if (session.body != null && !session.body.isRemoved()) session.body.discard();
		}
		REQUESTED.clear();
		REVEALED.clear();
		SESSIONS.clear();
		BODY_OWNERS.clear();
		ShadowConjurationManager.clear();
		ShadowPowerRuntime.clear();
		ShadowCombatController.clear();
		ShadowCommandRuntime.clear();
		ShadowStatusSync.clear();
		com.powers.knowledge.KnowledgeRemoteProviderRuntime.clear();
		com.powers.knowledge.MagicAttemptJournal.global().clear();
	}
}
