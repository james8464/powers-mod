package com.powers.companion.combat;

import com.powers.companion.ShadowCompanionEntity;
import com.powers.power.PowerEnergy;
import com.powers.power.state.GlobalTimeStopManager;
import com.powers.power.state.MagicShieldManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Owns toggle cleanup and hard per-tick execution budgets instead of gameplay cooldowns. */
public final class ShadowPowerRuntime {
	private record State(UUID body, Map<String, Long> toggles) { }
	private static final Map<UUID, State> STATES = new HashMap<>();
	private static final EnumMap<ShadowPowerAction.WorkClass, Integer> USED =
			new EnumMap<>(ShadowPowerAction.WorkClass.class);
	private static long budgetTick = Long.MIN_VALUE;
	private static long casts;

	private ShadowPowerRuntime() {
	}

	public static boolean tryReserve(ShadowPowerAction.WorkClass work, long tick) {
		if (tick != budgetTick) {
			USED.clear();
			budgetTick = tick;
		}
		int used = USED.getOrDefault(work, 0);
		if (used >= limit(work)) return false;
		USED.put(work, used + 1);
		return true;
	}

	public static void activate(UUID owner, UUID body, String id, long expiresAt) {
		State current = STATES.get(owner);
		Map<String, Long> toggles = new HashMap<>(current == null || !current.body().equals(body)
				? Map.of() : current.toggles());
		toggles.put(id, expiresAt);
		STATES.put(owner, new State(body, toggles));
	}

	public static boolean active(UUID owner, String id) {
		State state = STATES.get(owner);
		return state != null && state.toggles().containsKey(id);
	}

	/** Runs on the existing staggered five-tick companion pulse. */
	public static void tick(ServerPlayer owner, ShadowCompanionEntity shadow, long tick) {
		State state = STATES.get(owner.getUUID());
		if (state == null || !state.body().equals(shadow.getUUID())) return;
		Set<String> expired = new HashSet<>();
		state.toggles().entrySet().removeIf(entry -> {
			boolean remove = tick >= entry.getValue();
			if (remove) expired.add(entry.getKey());
			return remove;
		});
		if (!expired.isEmpty()) cleanup(owner, shadow, expired);
		if (state.toggles().isEmpty()) {
			STATES.remove(owner.getUUID());
			return;
		}
		if (tick % 20L == 0L) {
			int drain = state.toggles().keySet().stream()
					.filter(id -> !id.equals("time_freeze"))
					.mapToInt(PowerEnergy::baseCost).map(cost -> Math.max(1, cost / 4)).sum();
			if (shadow.energy() < drain) {
				clearOwner(owner, shadow);
				return;
			}
			shadow.setEnergy(shadow.energy() - drain);
		}
		if (state.toggles().containsKey("flight")) shadow.setNoGravity(true);
	}

	public static void stop(ServerPlayer owner, ShadowCompanionEntity shadow, String id) {
		State state = STATES.get(owner.getUUID());
		if (state != null) {
			state.toggles().remove(id);
			if (state.toggles().isEmpty()) STATES.remove(owner.getUUID());
		}
		cleanup(owner, shadow, Set.of(id));
	}

	public static void clearOwner(ServerPlayer owner, ShadowCompanionEntity shadow) {
		State removed = STATES.remove(owner.getUUID());
		cleanup(owner, shadow, removed == null ? Set.of() : Set.copyOf(removed.toggles().keySet()));
	}

	public static void forget(UUID owner) {
		STATES.remove(owner);
	}

	public static void clear() {
		STATES.clear();
		USED.clear();
		budgetTick = Long.MIN_VALUE;
		casts = 0L;
	}

	public static void recordCast() {
		casts++;
	}

	public record Diagnostics(int owners, int toggles, int budgetClassesUsed, long casts) { }

	public static Diagnostics diagnostics() {
		return new Diagnostics(STATES.size(), STATES.values().stream()
				.mapToInt(state -> state.toggles().size()).sum(), USED.size(), casts);
	}

	private static void cleanup(ServerPlayer owner, ShadowCompanionEntity shadow, Set<String> ids) {
		if (ids.contains("time_freeze")) GlobalTimeStopManager.stopShadow(owner);
		if (ids.contains("forcefield")) MagicShieldManager.global().remove(shadow.getUUID());
		if (ids.contains("size_shift")) shadow.getAttribute(Attributes.SCALE).setBaseValue(1.0);
		if (ids.contains("flight")) shadow.setNoGravity(false);
		if (ids.contains("invisibility")) shadow.removeEffect(MobEffects.INVISIBILITY);
		if (ids.contains("nightfall_dominion")) {
			shadow.removeEffect(MobEffects.STRENGTH);
			shadow.removeEffect(MobEffects.RESISTANCE);
			shadow.removeEffect(MobEffects.SPEED);
			shadow.removeEffect(MobEffects.REGENERATION);
		}
	}

	private static int limit(ShadowPowerAction.WorkClass work) {
		return switch (work) {
			case CHEAP -> 256;
			case ENTITY_QUERY -> 64;
			case PROJECTILE -> 64;
			case TERRAIN -> 12;
			case GLOBAL -> 1;
		};
	}
}
