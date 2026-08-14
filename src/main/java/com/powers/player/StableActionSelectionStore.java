package com.powers.player;

import com.powers.magic.runtime.MagicRuntime;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Migrates persisted spell and crystal selections from retired IDs to canonical action IDs. */
final class StableActionSelectionStore {
	private StableActionSelectionStore() {
	}

	static int selectedSpell(AttachmentTarget target, String ownerKey, List<String> actionIds) {
		return selected(target, ownerKey, actionIds, true);
	}

	static int selectedSpell(AttachmentTarget target, String ownerKey, int actionCount) {
		if (actionCount <= 0) return 0;
		return Math.floorMod(rawSpell(target, ownerKey), actionCount);
	}

	static int selectedCrystal(AttachmentTarget target, String ownerKey, List<String> actionIds) {
		return selected(target, ownerKey, actionIds, false);
	}

	static int selectedCrystal(AttachmentTarget target, String ownerKey, int actionCount) {
		if (actionCount <= 0) return 0;
		int selected = target.getAttachedOrElse(PlayerPowerAttachments.CRYSTAL_SELECTIONS, Map.of())
				.getOrDefault(ownerKey, 0);
		return com.powers.power.crystals.CrystalModeState.current(selected, actionCount);
	}

	private static int selected(AttachmentTarget target, String ownerKey, List<String> actionIds,
			boolean spell) {
		if (actionIds == null || actionIds.isEmpty()) return 0;
		Map<String, String> keys = target.getAttachedOrElse(spell
				? PlayerPowerAttachments.SPELL_SELECTION_KEYS
				: PlayerPowerAttachments.CRYSTAL_SELECTION_KEYS, Map.of());
		String saved = keys.get(ownerKey);
		if (saved != null) {
			var resolved = MagicRuntime.catalogue().snapshot().resolve(saved);
			int index = resolved == null ? -1 : actionIds.indexOf(resolved.value());
			if (index >= 0) {
				if (!resolved.value().equals(saved)) set(target, ownerKey, resolved.value(), spell);
				return index;
			}
		}
		return spell
				? PlayerPowers.get(target).selectedSpell(ownerKey, actionIds.size())
				: PlayerPowers.get(target).selectedCrystalMode(ownerKey, actionIds.size());
	}

	static void setSpell(AttachmentTarget target, String ownerKey, String actionId) {
		set(target, ownerKey, actionId, true);
	}

	static void setCrystal(AttachmentTarget target, String ownerKey, String actionId) {
		set(target, ownerKey, actionId, false);
	}

	static int rawSpell(AttachmentTarget target, String ownerKey) {
		return target.getAttachedOrElse(PlayerPowerAttachments.SPELL_SELECTIONS, Map.of())
				.getOrDefault(ownerKey, 0);
	}

	static int cycleSpell(AttachmentTarget target, String ownerKey, int actionCount) {
		if (actionCount <= 0) return 0;
		int selected = (selectedSpell(target, ownerKey, actionCount) + 1) % actionCount;
		setSpell(target, ownerKey, selected);
		return selected;
	}

	static void setSpell(AttachmentTarget target, String ownerKey, int selected) {
		setIndex(target, ownerKey, selected, true);
	}

	static void setCrystal(AttachmentTarget target, String ownerKey, int selected) {
		setIndex(target, ownerKey, selected, false);
	}

	private static void setIndex(AttachmentTarget target, String ownerKey, int selected, boolean spell) {
		Map<String, Integer> updated = new HashMap<>(target.getAttachedOrElse(spell
				? PlayerPowerAttachments.SPELL_SELECTIONS
				: PlayerPowerAttachments.CRYSTAL_SELECTIONS, Map.of()));
		updated.put(ownerKey, Math.max(0, selected));
		if (spell) target.setAttached(PlayerPowerAttachments.SPELL_SELECTIONS, updated);
		else target.setAttached(PlayerPowerAttachments.CRYSTAL_SELECTIONS, updated);
	}

	private static void set(AttachmentTarget target, String ownerKey, String actionId, boolean spell) {
		Map<String, String> updated = new HashMap<>(target.getAttachedOrElse(spell
				? PlayerPowerAttachments.SPELL_SELECTION_KEYS
				: PlayerPowerAttachments.CRYSTAL_SELECTION_KEYS, Map.of()));
		updated.put(ownerKey, actionId);
		if (spell) target.setAttached(PlayerPowerAttachments.SPELL_SELECTION_KEYS, updated);
		else target.setAttached(PlayerPowerAttachments.CRYSTAL_SELECTION_KEYS, updated);
	}
}
