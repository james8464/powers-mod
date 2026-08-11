package com.powers.migration;

import com.powers.player.PlayerPowers;
import com.powers.power.PowerAffinity;
import com.powers.power.PowerRegistry;

import java.util.ArrayList;
import java.util.List;

/** Deterministic, idempotent normalization for released player-save shapes. */
public final class SaveMigrationRules {
	private SaveMigrationRules() {
	}

	/**
	 * Preserves a genuinely unassigned new player, but repairs any non-empty
	 * partial, duplicate, oversized, forbidden, or retired innate loadout.
	 */
	public static List<String> canonicalPowerSlots(List<String> stored, PowerAffinity affinity) {
		if (stored == null || stored.isEmpty()) return List.of();
		PowerAffinity safeAffinity = affinity == null ? PowerAffinity.RADIANT : affinity;
		List<String> normalized = new ArrayList<>(PlayerPowers.SLOT_COUNT);
		for (String id : stored) {
			if (normalized.size() >= PlayerPowers.SLOT_COUNT) break;
			normalized.add(id == null ? "" : id);
		}
		while (normalized.size() < PlayerPowers.SLOT_COUNT) {
			normalized.add("powers:retired_missing_slot");
		}
		return PowerRegistry.reconcile(normalized, safeAffinity);
	}
}
