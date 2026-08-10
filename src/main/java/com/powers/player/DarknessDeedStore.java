package com.powers.player;

import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.powers.player.PlayerPowerAttachments.DARKNESS_DEEDS;

/** Typed persistence adapter for the stable string-keyed darkness deed attachment. */
final class DarknessDeedStore {
	private DarknessDeedStore() {
	}

	static Map<DarknessDeed, Integer> read(AttachmentTarget target) {
		EnumMap<DarknessDeed, Integer> result = new EnumMap<>(DarknessDeed.class);
		for (Map.Entry<String, Integer> entry
				: target.getAttachedOrElse(DARKNESS_DEEDS, Map.of()).entrySet()) {
			DarknessDeed deed = DarknessDeed.fromKey(entry.getKey());
			if (deed != null && entry.getValue() > 0) {
				result.put(deed, entry.getValue());
			}
		}
		return Map.copyOf(result);
	}

	static Map<DarknessDeed, Integer> increment(AttachmentTarget target, Set<DarknessDeed> deeds) {
		if (deeds.isEmpty()) {
			return read(target);
		}
		Map<String, Integer> updated = new HashMap<>(
				target.getAttachedOrElse(DARKNESS_DEEDS, Map.of()));
		for (DarknessDeed deed : deeds) {
			updated.compute(deed.key(), (key, current) -> incrementCounter(current));
		}
		target.setAttached(DARKNESS_DEEDS, updated);
		return read(target);
	}

	private static int incrementCounter(Integer current) {
		if (current == null || current < 0) {
			return 1;
		}
		return current == Integer.MAX_VALUE ? Integer.MAX_VALUE : current + 1;
	}
}
