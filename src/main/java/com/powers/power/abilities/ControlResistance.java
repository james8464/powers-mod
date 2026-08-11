package com.powers.power.abilities;

import com.powers.entity.FirstVessel;
import com.powers.entity.RealmHerald;
import com.powers.fx.PowerFx;
import com.powers.power.PowerDamage;
import com.powers.util.PowerMessages;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Explicit full/resisted/immune/reflected policy shared by hostile control powers. */
public final class ControlResistance {
	public static final String RESISTED_TAG = "powers.control_resisted";
	public static final String IMMUNE_TAG = "powers.control_immune";
	public static final String REFLECTED_TAG = "powers.control_reflected";
	private static final int FEEDBACK_COOLDOWN_TICKS = 20;
	private static final int MAX_FEEDBACK_TARGETS = 256;
	private static final Map<UUID, Long> LAST_FEEDBACK = new LinkedHashMap<>();

	/** Mutually exclusive authoritative control outcome. */
	public enum Outcome {
		FULL,
		RESISTED,
		IMMUNE,
		REFLECTED
	}

	private ControlResistance() {
	}

	/** Resolves explicit tags first; major bosses resist instead of silently ignoring control. */
	public static Outcome outcome(LivingEntity target) {
		var tags = target.entityTags();
		return fromFlags(tags.contains(REFLECTED_TAG), tags.contains(IMMUNE_TAG),
				tags.contains(RESISTED_TAG) || target instanceof FirstVessel
						|| target instanceof RealmHerald);
	}

	/** Pure priority rule used by tests and custom boss integrations. */
	public static Outcome fromFlags(boolean reflected, boolean immune, boolean resisted) {
		if (reflected) return Outcome.REFLECTED;
		if (immune) return Outcome.IMMUNE;
		return resisted ? Outcome.RESISTED : Outcome.FULL;
	}

	/** Applies the category's authored 30% duration for resistant bosses. */
	public static int adjustDuration(int ticks, Outcome outcome) {
		int safe = Math.max(0, ticks);
		return switch (outcome) {
			case FULL -> safe;
			case RESISTED -> safe == 0 ? 0 : Math.max(1, (int) Math.ceil(safe * 0.30));
			case IMMUNE, REFLECTED -> 0;
		};
	}

	/** Applies the category's authored 30% impulse for resistant bosses. */
	public static Vec3 adjustImpulse(Vec3 impulse, Outcome outcome) {
		if (impulse == null || !finite(impulse)) return Vec3.ZERO;
		return switch (outcome) {
			case FULL -> impulse;
			case RESISTED -> impulse.scale(0.30);
			case IMMUNE, REFLECTED -> Vec3.ZERO;
		};
	}

	/** Resolves and emits rate-limited semantic feedback; reflection visibly punishes the caster. */
	public static Outcome evaluate(ServerPlayer caster, LivingEntity target, String actionId) {
		Outcome outcome = outcome(target);
		if (outcome == Outcome.FULL || !(target.level() instanceof ServerLevel level)) return outcome;
		long now = level.getGameTime();
		Long last = LAST_FEEDBACK.get(target.getUUID());
		if (last != null && now - last < FEEDBACK_COOLDOWN_TICKS) return outcome;
		if (LAST_FEEDBACK.size() >= MAX_FEEDBACK_TARGETS) {
			var iterator = LAST_FEEDBACK.keySet().iterator();
			if (iterator.hasNext()) LAST_FEEDBACK.remove(iterator.next());
		}
		LAST_FEEDBACK.put(target.getUUID(), now);
		int color = switch (outcome) {
			case RESISTED -> 0xE2A34A;
			case IMMUNE -> 0xE9F4FF;
			case REFLECTED -> 0xA020F0;
			case FULL -> 0xFFFFFF;
		};
		PowerFx.rune(level, target.position().add(0, target.getBbHeight() * 0.55, 0), 1.2,
				color, 18, now * 0.08);
		PowerFx.sound(level, target.position(), SoundEvents.SHIELD_BLOCK.value(), 0.8f,
				outcome == Outcome.RESISTED ? 0.8f : 1.2f);
		if (caster != null) {
			PowerMessages.overlay(caster, net.minecraft.network.chat.Component.translatable(
					"ability.powers.control." + outcome.name().toLowerCase(), actionId));
			if (outcome == Outcome.REFLECTED && caster != target) {
				caster.hurtServer(level, PowerDamage.source(target), 6.0F);
				Vec3 away = caster.position().subtract(target.position()).normalize().scale(1.2);
				caster.push(away.x, 0.45, away.z);
			}
		}
		return outcome;
	}

	/** Clears process-local feedback limits during server shutdown. */
	public static void clear() {
		LAST_FEEDBACK.clear();
	}

	private static boolean finite(Vec3 vector) {
		return Double.isFinite(vector.x) && Double.isFinite(vector.y) && Double.isFinite(vector.z);
	}
}
