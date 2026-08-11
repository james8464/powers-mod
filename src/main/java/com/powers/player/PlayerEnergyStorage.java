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
			if (!(target instanceof ServerPlayer player)) return false;
			int reservoirBefore = ArtifactEnergyReservoir.totalStored(player);
			if (!ArtifactEnergyReservoir.payShortfall(player, amount - current)) return false;
			store(target, 0);
			PlayerEnergyHistory.record(player, EnergyHistorySource.PLAYER_POOL_COST, current, 0);
			PlayerEnergyHistory.record(player, EnergyHistorySource.RESERVOIR_COST, reservoirBefore,
					ArtifactEnergyReservoir.totalStored(player));
			return true;
		}
		store(target, current - amount);
		record(target, EnergyHistorySource.PLAYER_POOL_COST, current, current - amount);
		return true;
	}

	static void refund(AttachmentTarget target, int amount) {
		int before = energy(target);
		long updated = (long) before + Math.max(0L, (long) amount);
		int after = (int) Math.min(capacity(target), updated);
		store(target, after);
		record(target, EnergyHistorySource.REFUND, before, after);
	}

	static boolean regenerate(AttachmentTarget target, int amount) {
		if (target instanceof ServerPlayer player && player.hasEffect(PowersEffects.EXHAUSTION)) return false;
		int current = energy(target);
		int updated = Math.min(capacity(target), current + Math.max(0, amount));
		if (updated == current) return false;
		store(target, updated);
		record(target, EnergyHistorySource.REGENERATION, current, updated);
		return true;
	}

	static void restore(AttachmentTarget target) {
		if (target instanceof ServerPlayer player && player.hasEffect(PowersEffects.EXHAUSTION)) return;
		int before = energy(target);
		int after = capacity(target);
		store(target, after);
		record(target, EnergyHistorySource.SLEEP_RESTORE, before, after);
	}

	static void drain(AttachmentTarget target, int amount) {
		if (amount > 0 && !limitsDisabled(target)) {
			int before = energy(target);
			int after = Math.max(0, before - amount);
			store(target, after);
			record(target, EnergyHistorySource.DIRECT_DRAIN, before, after);
		}
	}

	static void empty(AttachmentTarget target) {
		if (!limitsDisabled(target)) {
			int before = energy(target);
			store(target, 0);
			record(target, EnergyHistorySource.EMPTY, before, 0);
		}
	}

	static void forceRestore(AttachmentTarget target) {
		int before = energy(target);
		int after = capacity(target);
		store(target, after);
		record(target, EnergyHistorySource.OPERATOR_RESTORE, before, after);
	}

	/** Moves energy between owned stores without reporting aggregate spend or restoration. */
	static void transferBalance(AttachmentTarget target, int value) {
		int before = energy(target);
		store(target, value);
		record(target, EnergyHistorySource.INTERNAL_TRANSFER, before, energy(target));
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

	private static void record(AttachmentTarget target, EnergyHistorySource source,
			int before, int after) {
		if (target instanceof ServerPlayer player) {
			PlayerEnergyHistory.record(player, source, before, after);
		}
	}
}
