package com.powers.companion;

import com.powers.PowersEntities;
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
import com.powers.power.Power;
import com.powers.power.PowerRegistry;
import com.powers.power.PowerTargeting;
import com.powers.companion.combat.ShadowPowerAction;
import com.powers.companion.combat.ShadowPowerCatalogue;
import com.powers.companion.combat.ShadowPowerExecutor;
import com.powers.companion.combat.ShadowPowerRuntime;
import com.powers.companion.combat.ShadowCombatController;
import com.powers.companion.combat.ShadowRequestRange;
import com.powers.network.NamedLivingTargetIndex;
import com.powers.network.NamedTargetRules;
import com.powers.util.LoadedChunks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
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
import java.util.List;
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
	private static final ShadowDialogueEngine DIALOGUE = new ShadowDialogueEngine();
	private static volatile ShadowNameResolver nameResolver;

	private static final class Session {
		private final long id;
		private ShadowCompanionEntity body;
		private final Set<UUID> apparitionViewers = new HashSet<>();
		private final ShadowTaskController tasks = new ShadowTaskController();

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
		if (!ShadowManifestationRules.mayRecall(data, player.level().getGameTime())) return;

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
			rememberFailure(player, taskState.reason());
			replyAndRemember(player, "", DIALOGUE.failure(taskState.reason()));
		}
		if (ShadowConjurationManager.active(ownerId)) tickConjuration(player, session);
		boolean teleported = ShadowCompanionStore.get(player).stance() == ShadowStance.FOLLOW
				&& followOwner(player, session);
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
		ShadowCompanionData stored = ShadowCompanionStore.get(owner);
		ShadowRequest request = ShadowRequestParser.parse(rawMessage, stored.memory(), names());
		if (!request.addressed()) return false;
		if (!eligible(owner)) {
			owner.sendSystemMessage(Component.literal(
					"Shadow is silent. Darkness and the Shadow Sword must both recognise you.")
					.withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
			return true;
		}
		if (REVEALED.contains(owner.getUUID())) broadcastAddress(owner, request.original());
		switch (request.kind()) {
			case EMPTY -> replyAndRemember(owner, request.original(), "Speak, and I will listen.");
			case TOO_LONG -> replyAndRemember(owner, "", "One thought at a time. Your request is too long.");
			case SUMMON -> summon(owner);
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
			case FOLLOW -> setStance(owner, ShadowStance.FOLLOW, request);
			case STAY -> setStance(owner, ShadowStance.STAY, request);
			case GUARD -> setStance(owner, ShadowStance.GUARD, request);
			case STOP -> stop(owner, request);
			case CLARIFY -> replyAndRemember(owner, request.original(), DIALOGUE.clarification(request));
			case DIAGNOSE, CONVERSE -> {
				REQUESTED.add(owner.getUUID());
				answer(owner, request.original());
			}
			case ATTACK, DEFEND, USE_POWER, STOP_POWER, GET_ITEM, CONJURE_ITEM, SCOUT,
					RANGE_PREFERENCE -> submit(owner, request);
			case NONE -> {
				return false;
			}
		}
		return true;
	}

	private static void setStance(ServerPlayer owner, ShadowStance stance, ShadowRequest request) {
		REQUESTED.add(owner.getUUID());
		ShadowCompanionStore.update(owner, state -> state.withStance(stance));
		replyAndRemember(owner, request.original(), DIALOGUE.accepted(request));
	}

	private static void stop(ServerPlayer owner, ShadowRequest request) {
		Session session = SESSIONS.get(owner.getUUID());
		if (session != null) {
			session.tasks.cancel("owner_stop");
			if (ShadowConjurationManager.active(owner.getUUID())) {
				ShadowConjurationManager.interrupt(owner, session.body, "owner_stop");
			}
		}
		ShadowCompanionStore.update(owner, state -> state.withStance(ShadowStance.FOLLOW));
		replyAndRemember(owner, request.original(), DIALOGUE.accepted(request));
	}

	private static void submit(ServerPlayer owner, ShadowRequest request) {
		REQUESTED.add(owner.getUUID());
		Session session = SESSIONS.get(owner.getUUID());
		if (session == null) {
			replyAndRemember(owner, request.original(), "Manifest me first; a command needs a body.");
			return;
		}
		long now = owner.level().getGameTime();
		ShadowTask task = ShadowTask.create(request.kind(), request.subject(), request.count(),
				now, now + taskLifetime(request.kind()), 0);
		ShadowTask.Result result = session.tasks.submit(task);
		if (!result.accepted()) {
			replyAndRemember(owner, request.original(), DIALOGUE.failure(result.reason()));
			return;
		}
		ShadowCompanionStore.update(owner, state -> state.withStance(ShadowStance.TASK)
				.withMemory(rememberReferent(state.memory(), request)));
		replyAndRemember(owner, request.original(), DIALOGUE.accepted(request));
		if (request.kind() == ShadowRequest.Kind.CONJURE_ITEM) {
			executeConjuration(owner, session, request);
		} else if (request.kind() == ShadowRequest.Kind.GET_ITEM) {
			executeRetrieval(owner, session, request);
		} else if (request.kind() == ShadowRequest.Kind.USE_POWER) {
			executePower(owner, session, request);
		} else if (request.kind() == ShadowRequest.Kind.STOP_POWER) {
			stopPower(owner, session, request);
		} else if (request.kind() == ShadowRequest.Kind.RANGE_PREFERENCE) {
			setRangePreference(owner, session, request);
		} else if (request.kind() == ShadowRequest.Kind.ATTACK
				|| request.kind() == ShadowRequest.Kind.DEFEND) {
			executeCombatOrder(owner, session, request);
		} else if (request.kind() == ShadowRequest.Kind.SCOUT) {
			executeScout(owner, session);
		}
	}

	private static void setRangePreference(ServerPlayer owner, Session session,
			ShadowRequest request) {
		ShadowRequestRange range = switch (request.range()) {
			case CLOSE -> ShadowRequestRange.CLOSE;
			case MID -> ShadowRequestRange.MID;
			case FAR -> ShadowRequestRange.FAR;
			case AUTO -> ShadowRequestRange.AUTO;
		};
		ShadowCompanionStore.update(owner, state -> state.withCombatRange(range));
		finishTask(owner, session, true, "range_preference_updated");
	}

	private static void executeCombatOrder(ServerPlayer owner, Session session,
			ShadowRequest request) {
		LivingEntity target = null;
		if (request.kind() == ShadowRequest.Kind.DEFEND) target = owner.getLastAttacker();
		if (target == null && !request.subject().isBlank()
				&& !request.subject().equals("owner")) {
			var resolution = NamedLivingTargetIndex.resolve(owner.level().getServer(), request.subject());
			if (resolution.status() == NamedTargetRules.Status.FOUND) target = resolution.target();
			else if (resolution.status() == NamedTargetRules.Status.AMBIGUOUS) {
				finishTask(owner, session, false, "ambiguous_target");
				return;
			}
		}
		if (target == null) target = PowerTargeting.findLivingTarget(owner, 128.0);
		if (target == null || target == owner || target == session.body) {
			finishTask(owner, session, false, "no_target");
			return;
		}
		session.body.setTarget(target);
		String targetName = target.getName().getString();
		ShadowCompanionStore.update(owner, state -> state.withMemory(state.memory()
				.rememberReferent(ShadowConversationMemory.ReferentType.ENTITY,
						targetName)));
		finishTask(owner, session, true, "combat_order_accepted");
	}

	private static void executeScout(ServerPlayer owner, Session session) {
		Vec3 look = owner.getLookAngle();
		Vec3 destination = owner.position().add(look.x * 24.0, 2.0, look.z * 24.0);
		session.body.getNavigation().moveTo(destination.x, destination.y, destination.z, 1.25);
		finishTask(owner, session, true, "scout_started");
	}

	private static void executePower(ServerPlayer owner, Session session, ShadowRequest request) {
		String id = request.subject().contains(":")
				? request.subject().substring(request.subject().indexOf(':') + 1) : request.subject();
		ShadowPowerAction action = ShadowPowerCatalogue.find(id);
		if (action == null) {
			finishTask(owner, session, false, "unknown_power");
			return;
		}
		LivingEntity target = session.body.getTarget();
		if (target == null || !target.isAlive()) target = PowerTargeting.findLivingTarget(owner, 128.0);
		var result = ShadowPowerExecutor.execute((ServerLevel) session.body.level(), session.body,
				target, action, new ShadowPowerExecutor.ExecutionContext(owner, true,
						owner.level().getServer().getTickCount()));
		finishTask(owner, session, result.success(), result.reason());
	}

	private static void stopPower(ServerPlayer owner, Session session, ShadowRequest request) {
		String id = request.subject().contains(":")
				? request.subject().substring(request.subject().indexOf(':') + 1) : request.subject();
		ShadowPowerRuntime.stop(owner, session.body, id);
		finishTask(owner, session, true, "power_stopped");
	}

	private static long taskLifetime(ShadowRequest.Kind kind) {
		return switch (kind) {
			case ATTACK, DEFEND -> 20L * 60L * 5L;
			case CONJURE_ITEM -> ShadowConjurationRules.DARK_CRYSTAL_CHANNEL_TICKS + 200L;
			case SCOUT, GET_ITEM -> 20L * 60L;
			default -> 20L * 20L;
		};
	}

	private static void executeConjuration(ServerPlayer owner, Session session,
			ShadowRequest request) {
		Identifier id = Identifier.tryParse(request.subject());
		if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) {
			finishTask(owner, session, false, "unknown_item");
			return;
		}
		ShadowConjurationManager.Outcome outcome = ShadowConjurationManager.begin(owner,
				session.body, BuiltInRegistries.ITEM.getValue(id), request.count());
		if (!outcome.accepted()) {
			finishTask(owner, session, false, outcome.reason());
		} else if (!outcome.pending()) {
			finishTask(owner, session, true, outcome.reason());
		}
	}

	private static void executeRetrieval(ServerPlayer owner, Session session,
			ShadowRequest request) {
		Identifier id = Identifier.tryParse(request.subject());
		if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) {
			finishTask(owner, session, false, "unknown_item");
			return;
		}
		var found = ShadowItemRetrieval.find((ServerLevel) session.body.level(),
				session.body.position(), BuiltInRegistries.ITEM.getValue(id),
				request.count(), owner.getUUID());
		if (found.isEmpty()) {
			finishTask(owner, session, false, "item_not_found_within_32_blocks");
			return;
		}
		ShadowItemRetrieval.deliver(owner, found.get(), request.count());
		finishTask(owner, session, true, "item_retrieved");
	}

	private static void tickConjuration(ServerPlayer owner, Session session) {
		ShadowConjurationManager.Outcome outcome = ShadowConjurationManager.tick(owner, session.body);
		if (outcome.pending()) return;
		finishTask(owner, session, outcome.accepted(), outcome.reason());
	}

	private static void finishTask(ServerPlayer owner, Session session,
			boolean success, String reason) {
		ShadowTask.Result result = success ? session.tasks.complete(reason) : session.tasks.fail(reason);
		ShadowCompanionStore.update(owner, state -> state.withStance(ShadowStance.FOLLOW)
				.withMemory(success ? state.memory()
						: state.memory().rememberFailure(reason)));
		String line = success ? switch (reason) {
			case "conjured" -> "It is shaped and delivered. Remember whose dark answered you.";
			case "dark_crystal_conjured" -> "The Dark Crystal is yours. The abyss now knows your hand.";
			case "item_retrieved" -> "I found the dropped item and placed it in your keeping.";
			default -> "The task is complete.";
		} : DIALOGUE.failure(result.reason());
		replyAndRemember(owner, "", line);
	}

	/** G-key compatibility: summon or dismiss without creating a chat message. */
	public static void interact(ServerPlayer owner, long request) {
		if (!eligible(owner)) return;
		if (request == -1L) summon(owner);
		else if (request == -2L) {
			REQUESTED.remove(owner.getUUID());
			REVEALED.remove(owner.getUUID());
			despawn(owner);
			replyPrivate(owner, "I return to the blade.");
		}
	}

	private static void summon(ServerPlayer owner) {
		ShadowCompanionData data = ShadowCompanionStore.get(owner);
		long now = owner.level().getGameTime();
		if (!ShadowManifestationRules.mayRecall(data, now)) {
			replyPrivate(owner, "The blade is still rebuilding what was broken.");
			return;
		}
		REQUESTED.add(owner.getUUID());
		if (data.revealed()) REVEALED.add(owner.getUUID());
		reply(owner, "I am beside you.");
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

	private static void answer(ServerPlayer owner, String question) {
		KnowledgeService.answerAsync(owner, question).thenAccept(answer ->
				owner.level().getServer().execute(() -> {
					if (owner.connection == null || owner.isRemoved()
							|| !REQUESTED.contains(owner.getUUID())) return;
					replyAndRemember(owner, question, spokenAnswer(answer));
				}));
	}

	private static void rememberFailure(ServerPlayer owner, String reason) {
		ShadowCompanionStore.update(owner, state -> state.withMemory(
				state.memory().rememberFailure(reason)));
	}

	private static void replyAndRemember(ServerPlayer owner, String request, String line) {
		ShadowCompanionStore.update(owner, state -> state.withMemory(
				state.memory().remember(request, line)));
		reply(owner, line);
	}

	private static ShadowConversationMemory rememberReferent(
			ShadowConversationMemory memory, ShadowRequest request) {
		ShadowConversationMemory.ReferentType type = switch (request.kind()) {
			case ATTACK, DEFEND -> ShadowConversationMemory.ReferentType.ENTITY;
			case USE_POWER, STOP_POWER -> ShadowConversationMemory.ReferentType.POWER;
			case GET_ITEM, CONJURE_ITEM -> ShadowConversationMemory.ReferentType.ITEM;
			default -> ShadowConversationMemory.ReferentType.TASK;
		};
		return memory.rememberReferent(type, request.subject())
				.rememberReferent(ShadowConversationMemory.ReferentType.TASK,
						request.kind().name().toLowerCase());
	}

	private static String spokenAnswer(KnowledgeAnswer answer) {
		String text = answer.answer().strip();
		return text.isEmpty() ? "That truth has not yet left a trace I can verify." : text;
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
			replyPrivate(owner, "This vessel is broken. Call me from the blade, and I will remember.");
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
		replyPrivate(owner, "This vessel is broken. Call me from the blade, and I will remember.");
	}

	private static void manifestBody(ServerLevel level, Vec3 position) {
		PowerFx.rune(level, position, 1.3, 0x55265F, 24, 0.0);
		PowerFx.burst(level, position.add(0.0, 0.9, 0.0),
				ParticleTypes.REVERSE_PORTAL, 18, 0.55, 0.02);
	}

	private static void collapse(ServerLevel level, Vec3 position) {
		PowerFx.rune(level, position, 1.4, 0x55265F, 24, Math.PI);
		PowerFx.burst(level, position.add(0.0, 0.9, 0.0),
				ParticleTypes.REVERSE_PORTAL, 26, 0.7, 0.03);
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

	private static void broadcastAddress(ServerPlayer owner, String line) {
		Component message = Component.literal("<" + owner.getScoreboardName() + "> shadow, " + line)
				.withStyle(ChatFormatting.GRAY);
		owner.level().getServer().getPlayerList().broadcastSystemMessage(message, false);
	}

	private static ShadowNameResolver names() {
		ShadowNameResolver cached = nameResolver;
		if (cached != null) return cached;
		Map<String, String> powers = new HashMap<>();
		for (Power power : PowerRegistry.getAll()) {
			String id = power.id().toString();
			powers.put(id, id);
			powers.put(power.id().getPath().replace('_', ' '), id);
			powers.putIfAbsent(power.name().getString(), id);
		}
		Map<String, String> items = new HashMap<>();
		BuiltInRegistries.ITEM.keySet().forEach(id -> {
			items.put(id.toString(), id.toString());
			items.putIfAbsent(id.getPath().replace('_', ' '), id.toString());
		});
		cached = ShadowNameResolver.from(powers, items);
		nameResolver = cached;
		return cached;
	}

	private static boolean eligible(ServerPlayer player) {
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
			com.powers.power.AmethystDampening.forget(removed.body);
			ShadowCompanionStore.update(owner, state -> state.withEnergy(removed.body.energy())
					.withRevealed(false).withoutBody());
			if (!removed.body.isRemoved()) removed.body.discard();
		}
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
		nameResolver = null;
		com.powers.knowledge.KnowledgeRemoteProviderRuntime.clear();
		com.powers.knowledge.MagicAttemptJournal.global().clear();
	}
}
