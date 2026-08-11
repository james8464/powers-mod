package com.powers.player;

import com.powers.PowersEffects;
import com.powers.power.PowerEnergy;
import com.powers.progression.PowerScalingService;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.minecraft.server.level.ServerPlayer;
import com.powers.testing.TestingOverrides;
import com.powers.item.ArtifactEnergyReservoir;

import static com.powers.player.PlayerPowerAttachments.DARKNESS_ENERGY;
import static com.powers.player.PlayerPowerAttachments.ENERGY;

/** Centralizes clamped reads and writes for the normal and Darkness energy pools. */
final class PlayerEnergyStorage {
	private PlayerEnergyStorage() {
	}

	static boolean usesDarkness(AttachmentTarget target) {
		return target instanceof ServerPlayer player && SkillSystem.hasDarknessTag(player);
	}

	static int capacity(AttachmentTarget target) {
		if (!(target instanceof ServerPlayer player)) return PowerEnergy.maxCapacity(0);
		int level = SkillSystem.effectiveLevel(player);
		int base = usesDarkness(target) ? PowerEnergy.darknessMaxCapacity(level)
				: PowerEnergy.maxCapacity(level);
		return PowerScalingService.energyCapacity(player, base);
	}

	static int energy(AttachmentTarget target) {
		boolean darkness = usesDarkness(target);
		int fallback = darkness
				? PowerEnergy.darknessMaxCapacity(0) : PowerEnergy.BASE_MAX;
		int stored = darkness
				? target.getAttachedOrElse(DARKNESS_ENERGY, fallback)
				: target.getAttachedOrElse(ENERGY, fallback);
		return Math.clamp(stored, 0, capacity(target));
	}

	static boolean consume(AttachmentTarget target, int amount) {
		if (amount <= 0) return true;
		if (limitsDisabled(target)) return true;
		int current = energy(target);
		if (current < amount) {
			if (!(target instanceof ServerPlayer player)
					|| !ArtifactEnergyReservoir.payShortfall(player, amount - current)) return false;
			store(target, 0);
			return true;
		}
		store(target, current - amount);
		return true;
	}

	static void refund(AttachmentTarget target, int amount) {
		long updated = (long) energy(target) + Math.max(0L, (long) amount);
		store(target, (int) Math.min(capacity(target), updated));
	}

	static boolean regenerate(AttachmentTarget target, int amount) {
		if (target instanceof ServerPlayer player && player.hasEffect(PowersEffects.EXHAUSTION)) return false;
		int current = energy(target);
		int updated = Math.min(capacity(target), current + Math.max(0, amount));
		if (updated == current) return false;
		store(target, updated);
		return true;
	}

	static void restore(AttachmentTarget target) {
		if (target instanceof ServerPlayer player && player.hasEffect(PowersEffects.EXHAUSTION)) return;
		store(target, capacity(target));
	}

	static void drain(AttachmentTarget target, int amount) {
		if (amount > 0 && !limitsDisabled(target)) store(target, energy(target) - amount);
	}

	static void empty(AttachmentTarget target) {
		if (!limitsDisabled(target)) store(target, 0);
	}

	static void forceRestore(AttachmentTarget target) {
		store(target, capacity(target));
	}

	static void store(AttachmentTarget target, int value) {
		int clamped = Math.clamp(value, 0, capacity(target));
		if (usesDarkness(target)) target.setAttached(DARKNESS_ENERGY, clamped);
		else target.setAttached(ENERGY, clamped);
	}

	private static boolean limitsDisabled(AttachmentTarget target) {
		return target instanceof ServerPlayer player
				&& TestingOverrides.energyDisabled(player.getUUID());
	}
}
