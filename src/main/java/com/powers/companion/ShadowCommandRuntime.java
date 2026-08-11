package com.powers.companion;

import com.powers.companion.combat.ShadowPowerAction;
import com.powers.companion.combat.ShadowPowerCatalogue;
import com.powers.companion.combat.ShadowPowerExecutor;
import com.powers.companion.combat.ShadowPowerRuntime;
import com.powers.companion.combat.ShadowRequestRange;
import com.powers.network.NamedLivingTargetIndex;
import com.powers.network.NamedTargetRules;
import com.powers.power.Power;
import com.powers.power.PowerRegistry;
import com.powers.power.PowerTargeting;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

/** Parses and executes one bounded foreground Shadow command independently of body lifecycle. */
final class ShadowCommandRuntime {
	private static final ShadowDialogueEngine DIALOGUE = new ShadowDialogueEngine();
	private static volatile ShadowNameResolver nameResolver;

	private ShadowCommandRuntime() {
	}

	static boolean handle(ServerPlayer owner, String rawMessage) {
		ShadowCompanionData stored = ShadowCompanionStore.get(owner);
		ShadowRequest request = ShadowRequestParser.parse(rawMessage, stored.memory(), names());
		if (!request.addressed()) return false;
		if (!PrivateCompanionManager.eligible(owner)) {
			owner.sendSystemMessage(Component.literal(
					"Shadow is silent. Darkness and the Shadow Sword must both recognise you.")
					.withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
			return true;
		}
		if (PrivateCompanionManager.isRevealed(owner.getUUID())) {
			ShadowCompanionMessaging.broadcastAddress(owner, request.original());
		}
		switch (request.kind()) {
			case EMPTY -> ShadowCompanionMessaging.replyAndRemember(
					owner, request.original(), "Speak, and I will listen.");
			case TOO_LONG -> ShadowCompanionMessaging.replyAndRemember(
					owner, "", "One thought at a time. Your request is too long.");
			case SUMMON -> PrivateCompanionManager.summon(owner);
			case DISMISS -> {
				PrivateCompanionManager.dismiss(owner);
				ShadowCompanionMessaging.replyPrivate(owner, "I return to the blade.");
			}
			case REVEAL -> {
				PrivateCompanionManager.request(owner);
				PrivateCompanionManager.setVisibility(owner, true);
				ShadowCompanionMessaging.reply(owner, "Let every witness see what follows you.");
			}
			case HIDE -> {
				PrivateCompanionManager.request(owner);
				PrivateCompanionManager.setVisibility(owner, false);
				ShadowCompanionMessaging.replyPrivate(owner, "Only you may see or hear me now.");
			}
			case FOLLOW -> setStance(owner, ShadowStance.FOLLOW, request);
			case STAY -> setStance(owner, ShadowStance.STAY, request);
			case GUARD -> setStance(owner, ShadowStance.GUARD, request);
			case STOP -> stop(owner, request);
			case CLARIFY -> ShadowCompanionMessaging.replyAndRemember(
					owner, request.original(), DIALOGUE.clarification(request));
			case DIAGNOSE, CONVERSE -> {
				PrivateCompanionManager.request(owner);
				ShadowCompanionMessaging.answer(owner, request.original());
			}
			case ATTACK, DEFEND, USE_POWER, STOP_POWER, GET_ITEM, CONJURE_ITEM, SCOUT,
					RANGE_PREFERENCE -> submit(owner, request);
			case NONE -> {
				return false;
			}
		}
		return true;
	}

	static void tickConjuration(ServerPlayer owner, PrivateCompanionManager.Session session) {
		ShadowConjurationManager.Outcome outcome = ShadowConjurationManager.tick(owner, session.body);
		if (!outcome.pending()) finishTask(owner, session, outcome.accepted(), outcome.reason());
	}

	static void clear() {
		nameResolver = null;
	}

	static String failure(String reason) {
		return DIALOGUE.failure(reason);
	}

	private static void setStance(ServerPlayer owner, ShadowStance stance, ShadowRequest request) {
		PrivateCompanionManager.request(owner);
		ShadowCompanionStore.update(owner, state -> state.withStance(stance));
		ShadowCompanionMessaging.replyAndRemember(owner, request.original(), DIALOGUE.accepted(request));
	}

	private static void stop(ServerPlayer owner, ShadowRequest request) {
		PrivateCompanionManager.Session session = PrivateCompanionManager.session(owner.getUUID());
		if (session != null) {
			session.tasks.cancel("owner_stop");
			if (ShadowConjurationManager.active(owner.getUUID())) {
				ShadowConjurationManager.interrupt(owner, session.body, "owner_stop");
			}
		}
		ShadowCompanionStore.update(owner, state -> state.withStance(ShadowStance.FOLLOW));
		ShadowCompanionMessaging.replyAndRemember(owner, request.original(), DIALOGUE.accepted(request));
	}

	private static void submit(ServerPlayer owner, ShadowRequest request) {
		PrivateCompanionManager.request(owner);
		PrivateCompanionManager.Session session = PrivateCompanionManager.session(owner.getUUID());
		if (session == null) {
			ShadowCompanionMessaging.replyAndRemember(
					owner, request.original(), "Manifest me first; a command needs a body.");
			return;
		}
		long now = owner.level().getGameTime();
		ShadowTask task = ShadowTask.create(request.kind(), request.subject(), request.count(),
				now, now + taskLifetime(request.kind()), 0);
		ShadowTask.Result result = session.tasks.submit(task);
		if (!result.accepted()) {
			ShadowCompanionMessaging.replyAndRemember(
					owner, request.original(), DIALOGUE.failure(result.reason()));
			return;
		}
		ShadowCompanionStore.update(owner, state -> state.withStance(ShadowStance.TASK)
				.withMemory(rememberReferent(state.memory(), request)));
		ShadowCompanionMessaging.replyAndRemember(owner, request.original(), DIALOGUE.accepted(request));
		switch (request.kind()) {
			case CONJURE_ITEM -> executeConjuration(owner, session, request);
			case GET_ITEM -> executeRetrieval(owner, session, request);
			case USE_POWER -> executePower(owner, session, request);
			case STOP_POWER -> stopPower(owner, session, request);
			case RANGE_PREFERENCE -> setRangePreference(owner, session, request);
			case ATTACK, DEFEND -> executeCombatOrder(owner, session, request);
			case SCOUT -> executeScout(owner, session);
			default -> { }
		}
	}

	private static void setRangePreference(ServerPlayer owner,
			PrivateCompanionManager.Session session, ShadowRequest request) {
		ShadowRequestRange range = switch (request.range()) {
			case CLOSE -> ShadowRequestRange.CLOSE;
			case MID -> ShadowRequestRange.MID;
			case FAR -> ShadowRequestRange.FAR;
			case AUTO -> ShadowRequestRange.AUTO;
		};
		ShadowCompanionStore.update(owner, state -> state.withCombatRange(range));
		finishTask(owner, session, true, "range_preference_updated");
	}

	private static void executeCombatOrder(ServerPlayer owner,
			PrivateCompanionManager.Session session, ShadowRequest request) {
		LivingEntity target = request.kind() == ShadowRequest.Kind.DEFEND
				? owner.getLastAttacker() : null;
		if (target == null && !request.subject().isBlank() && !request.subject().equals("owner")) {
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
				.rememberReferent(ShadowConversationMemory.ReferentType.ENTITY, targetName)));
		finishTask(owner, session, true, "combat_order_accepted");
	}

	private static void executeScout(ServerPlayer owner, PrivateCompanionManager.Session session) {
		Vec3 look = owner.getLookAngle();
		Vec3 destination = owner.position().add(look.x * 24.0, 2.0, look.z * 24.0);
		session.body.getNavigation().moveTo(destination.x, destination.y, destination.z, 1.25);
		finishTask(owner, session, true, "scout_started");
	}

	private static void executePower(ServerPlayer owner, PrivateCompanionManager.Session session,
			ShadowRequest request) {
		String id = localId(request.subject());
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

	private static void stopPower(ServerPlayer owner, PrivateCompanionManager.Session session,
			ShadowRequest request) {
		ShadowPowerRuntime.stop(owner, session.body, localId(request.subject()));
		finishTask(owner, session, true, "power_stopped");
	}

	private static void executeConjuration(ServerPlayer owner,
			PrivateCompanionManager.Session session, ShadowRequest request) {
		Identifier id = Identifier.tryParse(request.subject());
		if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) {
			finishTask(owner, session, false, "unknown_item");
			return;
		}
		var outcome = ShadowConjurationManager.begin(owner, session.body,
				BuiltInRegistries.ITEM.getValue(id), request.count());
		if (!outcome.accepted()) finishTask(owner, session, false, outcome.reason());
		else if (!outcome.pending()) finishTask(owner, session, true, outcome.reason());
	}

	private static void executeRetrieval(ServerPlayer owner,
			PrivateCompanionManager.Session session, ShadowRequest request) {
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

	private static void finishTask(ServerPlayer owner, PrivateCompanionManager.Session session,
			boolean success, String reason) {
		ShadowTask.Result result = success ? session.tasks.complete(reason) : session.tasks.fail(reason);
		ShadowCompanionStore.update(owner, state -> state.withStance(ShadowStance.FOLLOW)
				.withMemory(success ? state.memory() : state.memory().rememberFailure(reason)));
		String line = success ? switch (reason) {
			case "conjured" -> "It is shaped and delivered. Remember whose dark answered you.";
			case "dark_crystal_conjured" -> "The Dark Crystal is yours. The abyss now knows your hand.";
			case "item_retrieved" -> "I found the dropped item and placed it in your keeping.";
			default -> "The task is complete.";
		} : DIALOGUE.failure(result.reason());
		ShadowCompanionMessaging.replyAndRemember(owner, "", line);
	}

	private static ShadowConversationMemory rememberReferent(
			ShadowConversationMemory memory, ShadowRequest request) {
		ShadowConversationMemory.ReferentType type = switch (request.kind()) {
			case ATTACK, DEFEND -> ShadowConversationMemory.ReferentType.ENTITY;
			case USE_POWER, STOP_POWER -> ShadowConversationMemory.ReferentType.POWER;
			case GET_ITEM, CONJURE_ITEM -> ShadowConversationMemory.ReferentType.ITEM;
			default -> ShadowConversationMemory.ReferentType.TASK;
		};
		return memory.rememberReferent(type, request.subject()).rememberReferent(
				ShadowConversationMemory.ReferentType.TASK, request.kind().name().toLowerCase());
	}

	private static long taskLifetime(ShadowRequest.Kind kind) {
		return switch (kind) {
			case ATTACK, DEFEND -> 20L * 60L * 5L;
			case CONJURE_ITEM -> ShadowConjurationRules.DARK_CRYSTAL_CHANNEL_TICKS + 200L;
			case SCOUT, GET_ITEM -> 20L * 60L;
			default -> 20L * 20L;
		};
	}

	private static String localId(String id) {
		return id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
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
}
