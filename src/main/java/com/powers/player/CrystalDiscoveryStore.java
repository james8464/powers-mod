package com.powers.player;

import com.powers.magic.runtime.MagicRuntime;
import com.powers.power.crystals.CrystalDiscoveryRules;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;

/** Persistent per-player crystal discovery with one-way legacy selection migration. */
public final class CrystalDiscoveryStore {
	private CrystalDiscoveryStore() {
	}

	public static List<String> read(AttachmentTarget target, String crystalKey, List<String> modes) {
		Map<String, List<String>> all = target.getAttachedOrElse(
				PlayerPowerAttachments.CRYSTAL_DISCOVERIES, Map.of());
		List<String> saved = all.getOrDefault(crystalKey, List.of()).stream()
				.map(CrystalDiscoveryStore::resolve).toList();
		boolean hasLegacy = target.getAttachedOrElse(PlayerPowerAttachments.CRYSTAL_SELECTIONS, Map.of())
				.containsKey(crystalKey)
				|| target.getAttachedOrElse(PlayerPowerAttachments.CRYSTAL_SELECTION_KEYS, Map.of())
				.containsKey(crystalKey);
		OptionalInt legacy = hasLegacy && !all.containsKey(crystalKey)
				? OptionalInt.of(StableActionSelectionStore.selectedCrystal(target, crystalKey, modes))
				: OptionalInt.empty();
		List<String> normalized = CrystalDiscoveryRules.normalize(modes, saved, legacy);
		if ((all.containsKey(crystalKey) || hasLegacy) && !normalized.equals(all.get(crystalKey))) {
			write(target, crystalKey, normalized);
		}
		return normalized;
	}

	/** Marks a post-migration selector before it writes a new, still-unknown selection. */
	public static void initializeSelection(AttachmentTarget target, String crystalKey,
			List<String> modes) {
		Map<String, List<String>> all = target.getAttachedOrElse(
				PlayerPowerAttachments.CRYSTAL_DISCOVERIES, Map.of());
		if (all.containsKey(crystalKey)) return;
		List<String> migrated = read(target, crystalKey, modes);
		all = target.getAttachedOrElse(PlayerPowerAttachments.CRYSTAL_DISCOVERIES, Map.of());
		if (!all.containsKey(crystalKey)) write(target, crystalKey, migrated);
	}

	public static boolean discover(AttachmentTarget target, String crystalKey,
			List<String> modes, String actionId) {
		List<String> current = read(target, crystalKey, modes);
		List<String> updated = CrystalDiscoveryRules.discover(modes, current, resolve(actionId));
		if (updated.equals(current)) return false;
		write(target, crystalKey, updated);
		return true;
	}

	private static String resolve(String saved) {
		var resolved = saved == null ? null : MagicRuntime.catalogue().snapshot().resolve(saved);
		return resolved == null ? saved : resolved.value();
	}

	private static void write(AttachmentTarget target, String crystalKey, List<String> discovered) {
		Map<String, List<String>> updated = new HashMap<>(target.getAttachedOrElse(
				PlayerPowerAttachments.CRYSTAL_DISCOVERIES, Map.of()));
		updated.put(crystalKey, List.copyOf(discovered));
		target.setAttached(PlayerPowerAttachments.CRYSTAL_DISCOVERIES, Map.copyOf(updated));
	}
}
