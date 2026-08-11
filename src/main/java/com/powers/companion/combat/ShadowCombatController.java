package com.powers.companion.combat;

import com.powers.companion.ShadowCompanionEntity;
import com.powers.player.SkillSystem;
import com.powers.util.BoundedEntityCandidates;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Staggered controller: bounded target query, pure plan, one movement, optional one cast. */
public final class ShadowCombatController {
	public static final int MAX_TARGET_CANDIDATES = 64;
	public static final int CREDIT_WINDOW_TICKS = 100;
	private static final int PLAN_INTERVAL = 10;
	private static final int CAST_INTERVAL = 20;
	private static final Map<UUID, RuntimeState> STATES = new HashMap<>();

	public record TickResult(boolean acted, String learnedState,
			ShadowTacticalPlanner.Decision decision) { }

	private static final class RuntimeState {
		private final ShadowLearningState learning;
		private final BoundedCombatLearner learner;
		private long lastCastAt = Long.MIN_VALUE / 2L;
		private UUID creditTarget;
		private long creditReadyAt;

		private RuntimeState(String encoded) {
			learning = ShadowLearningState.decode(encoded);
			learner = new BoundedCombatLearner(learning);
		}
	}

	private ShadowCombatController() {
	}

	public static boolean shouldPlan(int tick, long sessionId) {
		return Math.floorMod(tick, PLAN_INTERVAL) == Math.floorMod(sessionId, PLAN_INTERVAL);
	}

	public static TickResult tick(ServerLevel level, ShadowCompanionEntity shadow,
			ServerPlayer owner, int serverTick, long sessionId, ShadowRequestRange preference,
			String encodedLearning) {
		if (!shouldPlan(serverTick, sessionId)) return new TickResult(false, "", null);
		RuntimeState state = STATES.computeIfAbsent(shadow.getUUID(), ignored ->
				new RuntimeState(encodedLearning));
		String updated = completeCredit(level, owner, shadow, state, serverTick);
		LivingEntity target = chooseTarget(level, owner, shadow);
		if (target == null) return new TickResult(false, updated, null);
		shadow.setTarget(target);

		ShadowCombatFacts facts = facts(owner, shadow, target, preference,
				allyInLane(level, owner, shadow, target));
		List<ShadowPowerAction> legal = new ArrayList<>(26);
		for (ShadowPowerAction action : ShadowPowerCatalogue.actions()) {
			if (action.cost() <= shadow.energy()) legal.add(action);
		}
		ShadowTacticalPlanner.Decision decision = ShadowTacticalPlanner.choose(legal, facts,
				state.learning);
		decision = maybeExplore(decision, legal, facts, shadow);
		move(shadow, owner, target, decision.movement());
		if (decision.action() == null || serverTick - state.lastCastAt < CAST_INTERVAL) {
			return new TickResult(false, updated, decision);
		}
		var result = ShadowPowerExecutor.execute(level, shadow, target, decision.action(),
				new ShadowPowerExecutor.ExecutionContext(owner, true, serverTick));
		if (!result.success()) return new TickResult(false, updated, decision);
		state.lastCastAt = serverTick;
		String context = facts.contextKey(decision.mode());
		String type = facts.archetype().name().toLowerCase();
		if (state.learner.openCredit(context, type, decision.action().id(), serverTick,
				ratio(target), ratio(owner), ratio(shadow))) {
			state.creditTarget = target.getUUID();
			state.creditReadyAt = serverTick + CREDIT_WINDOW_TICKS;
		}
		return new TickResult(true, updated, decision);
	}

	private static ShadowTacticalPlanner.Decision maybeExplore(
			ShadowTacticalPlanner.Decision chosen, List<ShadowPowerAction> legal,
			ShadowCombatFacts facts, ShadowCompanionEntity shadow) {
		boolean unsafe = facts.suppressed() || facts.boss() || facts.ownerHealthRatio() < .5
				|| facts.shadowHealthRatio() < .5 || facts.allyInFiringLane();
		if (chosen.action() == null || !BoundedCombatLearner.shouldExplore(
				shadow.getRandom().nextDouble(), unsafe, 0)) return chosen;
		for (ShadowPowerAction candidate : legal) {
			if (!candidate.id().equals(chosen.action().id())
					&& candidate.intent() == chosen.action().intent()) {
				return new ShadowTacticalPlanner.Decision(chosen.mode(), candidate,
						chosen.movement(), chosen.score(), chosen.evaluatedCount());
			}
		}
		return chosen;
	}

	public static void clearBody(UUID body) {
		STATES.remove(body);
	}

	public static void clear() {
		STATES.clear();
	}

	public record Diagnostics(int bodies, int contexts, int targetTypes, int creditWindows) { }

	public static Diagnostics diagnostics() {
		int contexts = 0;
		int types = 0;
		int credits = 0;
		for (RuntimeState state : STATES.values()) {
			contexts += state.learning.contextCount();
			types += state.learning.typeCount();
			if (state.learner.activeCredit()) credits++;
		}
		return new Diagnostics(STATES.size(), contexts, types, credits);
	}

	private static String completeCredit(ServerLevel level, ServerPlayer owner,
			ShadowCompanionEntity shadow, RuntimeState state, int tick) {
		if (!state.learner.activeCredit() || tick < state.creditReadyAt) return "";
		LivingEntity target = state.creditTarget == null ? null
				: level.getEntity(state.creditTarget) instanceof LivingEntity living ? living : null;
		state.learner.completeCredit(tick, target == null || !target.isAlive() ? 0.0 : ratio(target),
				ratio(owner), ratio(shadow));
		state.creditTarget = null;
		return state.learning.encode();
	}

	private static LivingEntity chooseTarget(ServerLevel level, ServerPlayer owner,
			ShadowCompanionEntity shadow) {
		LivingEntity[] preferredTargets = {
				shadow.getTarget(), owner.getLastAttacker(), owner.getLastHurtMob()};
		for (LivingEntity preferred : preferredTargets) {
			if (valid(owner, shadow, preferred)) return preferred;
		}
		return BoundedEntityCandidates.living(level,
				AABB.ofSize(owner.position(), 48.0, 24.0, 48.0), MAX_TARGET_CANDIDATES,
				entity -> entity instanceof Monster && valid(owner, shadow, entity),
				Comparator.comparingDouble(entity -> entity.distanceToSqr(owner))).stream()
				.findFirst().orElse(null);
	}

	private static boolean valid(ServerPlayer owner, ShadowCompanionEntity shadow,
			LivingEntity target) {
		return target != null && target.isAlive() && target != owner && target != shadow
				&& !target.entityTags().contains(SkillSystem.DARKNESS_TAG);
	}

	private static ShadowCombatFacts facts(ServerPlayer owner, ShadowCompanionEntity shadow,
			LivingEntity target, ShadowRequestRange preference, boolean allyInLane) {
		double attack = target.getAttributeValue(Attributes.ATTACK_DAMAGE);
		return new ShadowCombatFacts(Math.sqrt(shadow.distanceToSqr(target)), ratio(target),
				Math.clamp(attack / 24.0, 0.0, 1.0), target instanceof RangedAttackMob,
				target.getMaxHealth() >= 200.0F, ratio(owner), ratio(shadow),
				shadow.energy() / 1850.0, com.powers.companion.ShadowMagicState.actionsSuppressed(shadow),
				allyInLane, preference);
	}

	private static boolean allyInLane(ServerLevel level, ServerPlayer owner,
			ShadowCompanionEntity shadow, LivingEntity target) {
		Vec3 from = shadow.getEyePosition();
		Vec3 to = target.getEyePosition();
		AABB bounds = new AABB(from, to).inflate(1.5);
		return !BoundedEntityCandidates.living(level, bounds, 16,
				entity -> entity != shadow && entity != target
						&& (entity == owner || entity.entityTags().contains(SkillSystem.DARKNESS_TAG))
						&& distanceToSegmentSquared(entity.getEyePosition(), from, to) <= 1.5 * 1.5)
				.isEmpty();
	}

	private static void move(ShadowCompanionEntity shadow, ServerPlayer owner,
			LivingEntity target, ShadowTacticalPlanner.Movement movement) {
		switch (movement) {
			case APPROACH -> shadow.getNavigation().moveTo(target, 1.3);
			case ORBIT -> {
				Vec3 direction = target.position().subtract(shadow.position()).normalize();
				Vec3 side = new Vec3(-direction.z, 0.0, direction.x);
				Vec3 point = target.position().subtract(direction.scale(8.0)).add(side.scale(4.0));
				shadow.getNavigation().moveTo(point.x, point.y, point.z, 1.15);
			}
			case RETREAT -> {
				Vec3 away = shadow.position().subtract(target.position());
				if (away.lengthSqr() > 1.0E-6) shadow.setDeltaMovement(
						shadow.getDeltaMovement().add(away.normalize().scale(0.65)));
			}
			case INTERPOSE -> {
				Vec3 point = owner.position().add(target.position()).scale(0.5);
				shadow.getNavigation().moveTo(point.x, point.y, point.z, 1.35);
			}
			case RECOVER -> shadow.getNavigation().moveTo(owner, 1.2);
		}
	}

	private static double ratio(LivingEntity entity) {
		return entity.getMaxHealth() <= 0 ? 0.0
				: Math.clamp(entity.getHealth() / entity.getMaxHealth(), 0.0, 1.0);
	}

	private static double distanceToSegmentSquared(Vec3 point, Vec3 start, Vec3 end) {
		Vec3 segment = end.subtract(start);
		if (segment.lengthSqr() < 1.0E-8) return point.distanceToSqr(start);
		double t = Math.clamp(point.subtract(start).dot(segment) / segment.lengthSqr(), 0.0, 1.0);
		return point.distanceToSqr(start.add(segment.scale(t)));
	}
}
