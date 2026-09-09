package com.powers.companion;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Owner-keyed bounded callout state; observes only facts already held by the session. */
final class ShadowCalloutRuntime {
	private static final Map<UUID, State> STATES = new HashMap<>();
	private static long criticalWarnings;
	private static long chatterCallouts;
	private static long privateCallouts;
	private static long globalCallouts;

	private static final class State {
		private final EnumMap<ShadowCalloutPolicy.Kind, Long> lastByKind =
				new EnumMap<>(ShadowCalloutPolicy.Kind.class);
		private long lastChatterAt = Long.MIN_VALUE / 4L;
		private boolean combatActive;
	}

	private ShadowCalloutRuntime() { }

	static void observe(ServerPlayer owner, ShadowCompanionEntity shadow, long tick,
			boolean combatActive) {
		State state = STATES.computeIfAbsent(owner.getUUID(), ignored -> new State());
		ShadowCalloutMode mode = ShadowCompanionStore.get(owner).preferredCalloutMode();
		ShadowCalloutPolicy.Kind critical = critical(owner, shadow);
		boolean transition = combatActive != state.combatActive;
		if (critical != null && emit(owner, state, mode, critical, tick)) return;
		state.combatActive = combatActive;
		if (transition) emit(owner, state, mode, combatActive
				? ShadowCalloutPolicy.Kind.ENGAGE : ShadowCalloutPolicy.Kind.DISENGAGE, tick);
	}

	private static ShadowCalloutPolicy.Kind critical(ServerPlayer owner,
			ShadowCompanionEntity shadow) {
		if (owner.getMaxHealth() > 0.0F && owner.getHealth() / owner.getMaxHealth() <= 0.30F) {
			return ShadowCalloutPolicy.Kind.OWNER_DANGER;
		}
		LivingEntity attacker = owner.getLastAttacker();
		if (attacker != null && attacker.isAlive() && attacker.distanceToSqr(owner) <= 8.0 * 8.0) {
			return ShadowCalloutPolicy.Kind.INTRUSION;
		}
		if (ShadowMagicState.actionsSuppressed(shadow)) return ShadowCalloutPolicy.Kind.SUPPRESSED;
		return null;
	}

	private static boolean emit(ServerPlayer owner, State state, ShadowCalloutMode mode,
			ShadowCalloutPolicy.Kind kind, long tick) {
		long previous = state.lastByKind.getOrDefault(kind, Long.MIN_VALUE / 4L);
		if (!ShadowCalloutPolicy.shouldEmit(kind, mode, tick, previous, state.lastChatterAt)) {
			return false;
		}
		state.lastByKind.put(kind, tick);
		if (kind.critical()) criticalWarnings++;
		else {
			state.lastChatterAt = tick;
			chatterCallouts++;
		}
		if (PrivateCompanionManager.isRevealed(owner.getUUID())) globalCallouts++;
		else privateCallouts++;
		ShadowCompanionMessaging.callout(owner, ShadowCalloutPolicy.spoken(kind),
				ShadowCalloutPolicy.visual(kind), kind.critical());
		return true;
	}

	static void clearOwner(UUID owner) { STATES.remove(owner); }

	record Diagnostics(long criticalWarnings, long chatterCallouts,
			long privateCallouts, long globalCallouts) { }

	static Diagnostics diagnostics() {
		return new Diagnostics(criticalWarnings, chatterCallouts, privateCallouts, globalCallouts);
	}

	static void clear() {
		STATES.clear();
		criticalWarnings = 0L;
		chatterCallouts = 0L;
		privateCallouts = 0L;
		globalCallouts = 0L;
	}
}
